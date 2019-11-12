package com.github.flakm

import com.github.flakm.SSLProvider.{Secured, SecurityContext}
import com.typesafe.config.Config
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
  private def getSSLContext(path: String): SecurityContext = {
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

    Secured(sconf, sslContext)
  }

  lazy val serverSSLContext: SecurityContext = getSSLContext(
    "inmemoryldap.serverSsl"
  )
  lazy val clientSSLContext: SecurityContext = getSSLContext(
    "inmemoryldap.clientSsl"
  )
}

object SSLProvider {
  sealed trait SecurityContext

  case class KeystoreData(path: String, pass: String, storeType: String)

  case object NotSecure extends SecurityContext

  case class Secured(settings: SSLConfigSettings, sslContext: SSLContext)
      extends SecurityContext {
    def getTrustores: Seq[KeystoreData] =
      settings.trustManagerConfig.trustStoreConfigs.flatMap { t =>
        for {
          path <- t.filePath
          pass <- t.password
        } yield KeystoreData(path, pass, t.storeType)
      }

    def getKeystores: Seq[KeystoreData] =
      settings.keyManagerConfig.keyStoreConfigs.flatMap { k =>
        for {
          path <- k.filePath
          pass <- k.password
        } yield KeystoreData(path, pass, k.storeType)
      }
  }
}
