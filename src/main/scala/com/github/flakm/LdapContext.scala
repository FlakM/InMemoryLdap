package com.github.flakm

import java.net.InetAddress

import com.unboundid.ldap.listener.{InMemoryDirectoryServer, ReadOnlyInMemoryDirectoryServerConfig}
import com.unboundid.ldap.sdk.LDAPConnection
import javax.net.ssl.SSLContext

trait LdapContext {
  def port: Int
  def host: InetAddress
  def connectionFactory: () => LDAPConnection
  def shutDown(): Unit
  def config: ReadOnlyInMemoryDirectoryServerConfig
  def clientSslContext: Option[SSLContext]
}

object LdapContext {

  def apply(s: InMemoryDirectoryServer, clientSSLContext: Option[SSLContext]): LdapContext = new LdapContext {
    override def port: Int = s.getListenPort

    override def host: InetAddress = s.getListenAddress

    override def connectionFactory: () => LDAPConnection = s.getConnection

    override def shutDown(): Unit = s.shutDown(true)

    override def config: ReadOnlyInMemoryDirectoryServerConfig = s.getConfig

    override def equals(obj: Any): Boolean = obj match {
      case ldap: LdapContext => ldap.config == config
      case _                 => false
    }

    override def clientSslContext: Option[SSLContext] = clientSSLContext
  }

}
