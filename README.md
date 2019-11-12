# InMemoryLdap

[![Build Status](https://travis-ci.com/FlakM/InMemoryLdap.svg?branch=master)](https://travis-ci.com/FlakM/InMemoryLdap)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/3c64274de3ad4a13a3781956532ec08e)](https://www.codacy.com/manual/FlakM/InMemoryLdap?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=FlakM/InMemoryLdap&amp;utm_campaign=Badge_Grade)
[![Codacy Badge](https://api.codacy.com/project/badge/Coverage/3c64274de3ad4a13a3781956532ec08e)](https://www.codacy.com/manual/FlakM/InMemoryLdap?utm_source=github.com&utm_medium=referral&utm_content=FlakM/InMemoryLdap&utm_campaign=Badge_Coverage)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.flakm/inmemoryldap_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.flakm/inmemoryldap_2.12)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Library that helps with in-memory testing against ldap using [UnboundID LDAP SDK for Java](https://ldap.com/unboundid-ldap-sdk-for-java/).

Inspired by [embedded-kafka](https://github.com/embeddedkafka/embedded-kafka)

## Version capability

`inmemoryldap` is available in MavenCentral compiled for scala 2.12 and 2.13.
To use add project dependency using the tool of your choice.

### sbt

```sbt
libraryDependencies += "com.github.flakm" %% "inmemoryldap" % version
```

### gradle

```groovy
compile group: 'com.github.flakm', name: 'inmemoryldap_2.13', version: version
```

## How to use
By default configuration for mock will be taken from [reference.conf](src/main/resources/reference.conf).
Library will look for ldif definitions on classpath by path `/ldap/test-data.ldif`.
Multiple files can be scheduled by manipulating `inmemoryldap.files` config property.
Ldif files will be loaded to server in order of occurrence before running any user code. 
There are two alternatives of running embedded ldap:

### closure style

```scala
  import InMemoryLdapServer._
  withRunningLdap { implicit ctx =>
    // ldap will be accessible here on default address: 127.0.0.1:1234
  }
  // before going further all resources will be pruned
```

### start stop style

On the other hand you might want to use your mocked ldap for longer periods of time (ie with trait `BeforeAndAfterAll`).
This style might be easily used with java code.
To do so you might use: 

```scala
    val server: LdapContext = InMemoryLdapServer.start()
    // some code goes here  
    InMemoryLdapServer.stop()
```

## Disabling ssl

By default ldap server will be using ssl. SSL context will be created using trustore and keystores found on classpath in directory `/ssl`.
They are created using [create_keys.sh](src/test/resources/create_keys.sh). You might disable ssl for server by setting property `inmemoryldap.ssl.enabled = false`

## Additional settings

Both `com.github.flakm.InMemoryLdapServer.start` and `com.github.flakm.InMemoryLdapServer.withRunningLdapConfig` take optional config:

```scala
 withRunningLdapConfig(customConfig) { ctx =>
  //code here
 }
```

You might override those settings by creating your own application.conf under test resources or by using other options described [here](https://github.com/lightbend/config).
For more examples read [here](src/test/scala/com/github/flakm/WithRunningLdapTest.scala) or [here](src/test/scala/com/github/flakm/StartStopTest.scala)
