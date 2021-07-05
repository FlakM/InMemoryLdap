package com.github.flakm

import java.net.{InetAddress, URL}
import com.github.flakm.SSLProvider.{NotSecure, Secured, SecurityContext}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.Logger
import com.unboundid.ldap.listener.{
  InMemoryDirectoryServer,
  InMemoryDirectoryServerConfig,
  InMemoryListenerConfig
}

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

object InMemoryLdapServer {
  private val nil = null

  private val log = Logger(getClass)

  def defaultConfig: Config = ConfigEnricher.enrichConfig(ConfigFactory.load())

  private case class ServerWithContext(
      server: InMemoryDirectoryServer,
      secContext: SecurityContext
  )

  @volatile private var server: ServerWithContext = _

  private def defaultSettings(
      config: Config
  ): (InMemoryDirectoryServerConfig, SecurityContext) = {
    val imdsc = new InMemoryDirectoryServerConfig(
      config.getString("inmemoryldap.baseDn")
    )
    imdsc.addAdditionalBindCredentials(
      config.getString("inmemoryldap.admin.dn"),
      config.getString("inmemoryldap.admin.password")
    )

    val (
      serverSocketFactory,
      clientSocketFactory,
      startTLSSocketFactory,
      clientContext
    ) =
      if (config.getBoolean("inmemoryldap.ssl.enabled")) {
        val sslProvider = new SSLProvider(config)
        (sslProvider.serverSSLContext, sslProvider.clientSSLContext) match {
          case (Secured(_, s), clientContext @ Secured(_, c)) =>
            (
              s.getServerSocketFactory,
              c.getSocketFactory,
              c.getSocketFactory,
              clientContext
            )
          case _ =>
            throw new IllegalArgumentException(
              s"inmemoryldap.ssl.enabled set to true but stores are not configured correctly!"
            )
        }
      } else (nil, nil, nil, NotSecure)

    val listenerConf = new InMemoryListenerConfig(
      config.getString("inmemoryldap.listenerName"),
      InetAddress.getByName(config.getString("inmemoryldap.listenAddress")),
      config.getInt("inmemoryldap.listenPort"),
      serverSocketFactory,
      clientSocketFactory,
      startTLSSocketFactory
    )
    imdsc.setListenerConfigs(listenerConf)
    (imdsc, clientContext)
  }

  /**
    * beware that server reference is mutable
    *
    * @param config - optional config
    * @return mutable, shared reference to imds
    */
  def start(
      config: Config = defaultConfig
  ): LdapContext = {
    this.synchronized {
      if (server == nil) {
        server = startInternal(config)
      } else {
        log.warn("Server already running")
      }
      LdapContext(server.server, server.secContext)
    }
  }

  private def startInternal(
      config: Config = ConfigFactory.defaultReference
  ): ServerWithContext = {
    val (settings, sslContext) = defaultSettings(config)
    val ds: InMemoryDirectoryServer = new InMemoryDirectoryServer(
      settings
    )
    val allFilesFromConfiguration =
      config.getStringList("inmemoryldap.files").asScala.toList
    lazy val constructPathsFromFiles: List[String] => List[String] =
      aListOfFileNames => {
        aListOfFileNames
          .map(
            aSingleFileName => {
              Try(getClass().getResource(aSingleFileName)) match {
                case Success(anResource) => anResource.getPath
                case Failure(exc) =>
                  throw new Exception(
                    s"The ldiff file defined in configuration $aSingleFileName does not exist. ",
                    exc
                  )
              }
            }
          )
          .filter(x => x.nonEmpty)
      }

    val dataToImport: List[String] =
      constructPathsFromFiles.apply(allFilesFromConfiguration)
    dataToImport.foreach { path =>
      log.debug(s"Installing ldif file from ${path}")
      ds.importFromLDIF(false, path)
    }

    ds.startListening()
    log.debug(
      "Running in memory ldap server on {}:{}",
      ds.getListenAddress,
      ds.getListenPort
    )
    ServerWithContext(ds, sslContext)
  }

  def stop(): Unit = {
    this.synchronized {
      if (server != nil) {
        server.server.shutDown(true)
        server = nil
      } else {
        log.warn("ldap server cannot be closed as it is not running")
      }
    }
  }

  def withRunningLdap[T](body: => LdapContext => T): T =
    withRunningLdapConfig()(body)

  def withRunningLdapConfig[T](
      config: Config = defaultConfig
  )(body: => LdapContext => T): T = {
    val internal         = startInternal(config)
    val ctx: LdapContext = LdapContext(internal.server, internal.secContext)
    try {
      body(ctx)
    } finally {
      ctx.shutDown()
      log.debug("In memory ldap server closed")
    }
  }
}
