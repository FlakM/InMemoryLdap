package com.github.flakm

import java.net.InetAddress

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.Logger
import com.unboundid.ldap.listener.{
  InMemoryDirectoryServer,
  InMemoryDirectoryServerConfig,
  InMemoryListenerConfig
}

import scala.jdk.CollectionConverters._

object InMemoryLdapServer {

  private val log = Logger(getClass)

  private def defaultSettings(config: Config): InMemoryDirectoryServerConfig = {
    val imdsc = new InMemoryDirectoryServerConfig(
      config.getString("inmemoryldap.baseDn")
    )
    imdsc.addAdditionalBindCredentials(
      config.getString("inmemoryldap.admin.dn"),
      config.getString("inmemoryldap.admin.password")
    )
    val serverSocketFactory = null
    val listenerConf = new InMemoryListenerConfig(
      config.getString("inmemoryldap.listenerName"),
      InetAddress.getByName(config.getString("inmemoryldap.listenAddress")),
      config.getInt("inmemoryldap.listenPort"),
      serverSocketFactory,
      serverSocketFactory,
      serverSocketFactory
    )
    imdsc.setListenerConfigs(listenerConf)
    imdsc
  }

  @volatile private var server: InMemoryDirectoryServer = _

  /**
    * beware that server reference is mutable
    *
    * @param config - optional config
    * @return mutable, shared reference to imds
    */
  def start(
      config: Config = ConfigFactory.defaultReference
  ): InMemoryDirectoryServer = {
    this.synchronized {
      if (server == null) {
        server = startInternal(config)
      } else {
        log.warn("Server already running")
      }
      server
    }
  }

  private def startInternal(config: Config = ConfigFactory.defaultReference) = {
    val ds: InMemoryDirectoryServer = new InMemoryDirectoryServer(
      defaultSettings(config)
    )

    config
      .getStringList("inmemoryldap.files")
      .asScala
      .map(getClass.getResource(_).getPath)
      .foreach { path =>
        log.debug("Installing ldif file from {}", path)
        ds.importFromLDIF(false, path)
      }

    ds.startListening()
    log.debug(
      "Running in memory ldap server on {}:{}",
      ds.getListenAddress,
      ds.getListenPort
    )
    ds
  }

  def stop(): Unit = {
    this.synchronized {
      if (server != null) {
        server.shutDown(true)
        server = null
      } else {
        log.warn("ldap server cannot be closed as it is not running")
      }
    }
  }

  def withRunningLdap[T](body: => InMemoryDirectoryServer => T): T =
    withRunningLdapConfig()(body)

  def withRunningLdapConfig[T](
      config: Config = ConfigFactory.defaultReference()
  )(body: => InMemoryDirectoryServer => T): T = {
    val ds: InMemoryDirectoryServer = startInternal(config)
    try {
      body(ds)
    } finally {
      ds.shutDown(true)
      log.debug("In memory ldap server closed")
    }
  }

}
