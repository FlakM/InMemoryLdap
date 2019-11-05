package com.github.flakm

import com.unboundid.ldap.listener.InMemoryDirectoryServer
import com.unboundid.ldap.sdk.SearchResultEntry
import org.scalatest.Matchers

object CommonUtils extends Matchers {

  private def checkUserExists(
      user: String
  )(implicit ds: InMemoryDirectoryServer): Unit = {
    val conn = ds.getConnection
    val entry: SearchResultEntry =
      conn.getEntry(s"uid=$user,ou=people,dc=example,dc=com")
    entry.getAttribute("cn").getValue shouldBe user
  }

  def checkBobExists(implicit ds: InMemoryDirectoryServer): Unit =
    checkUserExists("bob")
  def checkLilyExists(implicit ds: InMemoryDirectoryServer): Unit =
    checkUserExists("lily")

}
