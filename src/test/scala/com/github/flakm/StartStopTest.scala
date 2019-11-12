package com.github.flakm

import com.github.flakm.CommonUtils._
import com.unboundid.ldap.sdk.LDAPException
import org.scalatest.{FlatSpec, Matchers}

class StartStopTest extends FlatSpec with Matchers {
  behavior of "InMemoryLdapServer.start/stop"

  it should "enable to start/stop shared server" in {
    val server: LdapContext = InMemoryLdapServer.start()

    checkBobExists(server)

    InMemoryLdapServer.start() shouldEqual server

    InMemoryLdapServer.stop()

    withClue("server already closed") {
      assertThrows[LDAPException](checkBobExists(server))
    }

    val nextServer = InMemoryLdapServer.start()
    nextServer should not equal server
    checkBobExists(nextServer)

    withClue("server still closed") {
      assertThrows[LDAPException](checkBobExists(server))
    }

    InMemoryLdapServer.stop()
    InMemoryLdapServer.stop() // we can call it multiple times
  }
}
