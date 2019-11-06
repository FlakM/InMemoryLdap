package com.github.flakm

import java.net.InetAddress

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.Logger
import com.unboundid.ldap.listener.{
  InMemoryDirectoryServer,
  InMemoryDirectoryServerConfig,
  InMemoryListenerConfig
}
import javax.net.ssl.SSLContext

object InMemoryLdapServer {

  private val nil = null

  private val log = Logger(getClass)

  def defaultConfig: Config = ConfigEnricher.enrichConfig(ConfigFactory.load())

  private def defaultSettings(
      config: Config
  ): (InMemoryDirectoryServerConfig, Option[SSLContext]) = {
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
        (
          sslProvider.serverSSLContext.getServerSocketFactory,
          sslProvider.clientSSLContext.getSocketFactory,
          sslProvider.clientSSLContext.getSocketFactory,
          Some(sslProvider.clientSSLContext)
        )
      } else (nil, nil, nil, None)

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

  @volatile private var server: (InMemoryDirectoryServer, Option[SSLContext]) =
    _

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
      LdapContext(server._1, server._2)
    }
  }

  private def startInternal(
      config: Config = ConfigFactory.defaultReference
  ): (InMemoryDirectoryServer, Option[SSLContext]) = {
    val (settings, sslContext) = defaultSettings(config)
    val ds: InMemoryDirectoryServer = new InMemoryDirectoryServer(
      settings
    )

    config.getStringList("inmemoryldap.files").forEach { p =>
      val path = getClass.getResource(p).getPath
      log.debug("Installing ldif file from {}", path)
      ds.importFromLDIF(false, path)
    }

    ds.startListening()
    log.debug(
      "Running in memory ldap server on {}:{}",
      ds.getListenAddress,
      ds.getListenPort
    )
    (ds, sslContext)
  }

  def stop(): Unit = {
    this.synchronized {
      if (server != nil) {
        server._1.shutDown(true)
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
    val (server, context) = startInternal(config)
    val ctx: LdapContext  = LdapContext(server, context)
    try {
      body(ctx)
    } finally {
      ctx.shutDown()
      log.debug("In memory ldap server closed")
    }
  }
}
