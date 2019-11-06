package com.github.flakm

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.sslconfig.ssl.{
  ConfigSSLContextBuilder,
  DefaultKeyManagerFactoryWrapper,
  DefaultTrustManagerFactoryWrapper,
  SSLConfigFactory,
  SSLConfigSettings
}
import com.typesafe.sslconfig.util.PrintlnLogger
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

class SSLProvider(config: Config) {

  private def getSSLContext(path: String): SSLContext = {

    val sconf: SSLConfigSettings = SSLConfigFactory.parse(
      config.getConfig(path).withFallback(config.getConfig("ssl-config"))
    )

    val kmfw = new DefaultKeyManagerFactoryWrapper(
      KeyManagerFactory.getDefaultAlgorithm
    )
    val tmfw = new DefaultTrustManagerFactoryWrapper(
      TrustManagerFactory.getDefaultAlgorithm
    )

    val builder =
      new ConfigSSLContextBuilder(PrintlnLogger.factory(), sconf, kmfw, tmfw)

    val sslContext = builder.build()

    sslContext
  }

  lazy val serverSSLContext: SSLContext = getSSLContext(
    "inmemoryldap.serverSsl"
  )
  lazy val clientSSLContext: SSLContext = getSSLContext(
    "inmemoryldap.clientSsl"
  )

}
