package com.github.flakm

import com.github.flakm.CommonUtils.{checkBobExists, _}
import com.github.flakm.InMemoryLdapServer._
import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpec, Matchers}

class WithRunningLdapTest extends FlatSpec with Matchers {

  behavior of "withRunningLdap"

  it should "enable default settings" in {
    withRunningLdap { implicit ds =>
      checkBobExists
    }
  }

  it should "enable changing of configuration" in {
    val defaultConf = ConfigFactory.defaultReference()

    withRunningLdapConfig(defaultConf) { implicit ds =>
      withClue("default configuration runs only test-data.ldif") {
        assertThrows[NullPointerException](checkLilyExists)
      }
    }

    val withAdditionalScript =
      ConfigFactory.parseString("""
        |inmemoryldap {
        |  files = [
        |    "/ldap/test-data.ldif"
        |    "/ldap/add_lily.ldif"
        |  ]
        |}
        |""".stripMargin).withFallback(defaultConf)

    withRunningLdapConfig(withAdditionalScript) { implicit ds =>
      checkLilyExists
    }

  }

}
