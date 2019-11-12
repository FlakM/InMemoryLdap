package com.github.flakm

import com.typesafe.config.{Config, ConfigFactory}

object ConfigEnricher {
  def enrichConfig(resolved: Config): Config = {
    if (resolved.getBoolean("inmemoryldap.ssl.resolveStoresFromClasspath")) {
      val dir = getClass.getResource("/ssl").getPath
      val withValueFromClasspath =
        ConfigFactory.parseString(s"inmemoryldap.ssl.directory=$dir")
      val overriddenReference = ConfigFactory
        .defaultReferenceUnresolved()
        .resolveWith(withValueFromClasspath)
      overriddenReference
        .withFallback(resolved)
    } else {
      resolved
    }
  }
}
