package com.github.flakm

import java.net.InetAddress

import com.github.flakm.SSLProvider.SecurityContext
import com.unboundid.ldap.listener.{
  InMemoryDirectoryServer,
  ReadOnlyInMemoryDirectoryServerConfig
}
import com.unboundid.ldap.sdk.LDAPConnection

trait LdapContext {
  def port: Int
  def host: InetAddress
  def connectionFactory: () => LDAPConnection
  def shutDown(): Unit
  def config: ReadOnlyInMemoryDirectoryServerConfig
  def securityContext: SecurityContext
}

object LdapContext {
  def apply(
      s: InMemoryDirectoryServer,
      clientSSLContext: SecurityContext
  ): LdapContext = new LdapContext {
    override def port: Int = s.getListenPort

    override def host: InetAddress = s.getListenAddress

    override def connectionFactory: () => LDAPConnection = s.getConnection

    override def shutDown(): Unit = s.shutDown(true)

    override def config: ReadOnlyInMemoryDirectoryServerConfig = s.getConfig

    override def equals(obj: Any): Boolean = obj match {
      case ldap: LdapContext => ldap.config == config
      case _                 => false
    }

    override def securityContext: SecurityContext = clientSSLContext
  }
}
