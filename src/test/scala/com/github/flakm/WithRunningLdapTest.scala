package com.github.flakm

import com.github.flakm.CommonUtils.{checkBobExists, _}
import com.github.flakm.InMemoryLdapServer._
import com.github.flakm.SSLProvider.{NotSecure, Secured}
import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpec, Matchers}

class WithRunningLdapTest extends FlatSpec with Matchers {
  behavior of "withRunningLdap"

  it should "enable default settings" in {
    withRunningLdap { implicit ds =>
      ds.securityContext match {
        case s: Secured =>
          s.getKeystores should not be empty
          s.getTrustores should not be empty
        case _ => fail("This should not happen")
      }
      checkBobExists
    }
  }

  it should "enable changing of configuration" in {
    val defaultConf = ConfigEnricher.enrichConfig(ConfigFactory.load())

    withRunningLdapConfig(defaultConf) { implicit ds =>
      withClue("default configuration runs only test-data.ldif") {
        assertThrows[NullPointerException](checkLilyExists)
      }
    }

    val withAdditionalScript =
      ConfigFactory.parseString("""
        |inmemoryldap {
        |  ssl.enabled = false
        |  files = [
        |    "/ldap/test-data.ldif"
        |    "/ldap/add_lily.ldif"
        |  ]
        |}
        |""".stripMargin).withFallback(defaultConf)

    withRunningLdapConfig(withAdditionalScript) { implicit ds =>
      ds.securityContext shouldBe NotSecure
      checkLilyExists
    }
  }
}
