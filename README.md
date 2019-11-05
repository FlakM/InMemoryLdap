# InMemoryLdap

[![Build Status](https://travis-ci.com/FlakM/InMemoryLdap.svg?branch=master)](https://travis-ci.com/FlakM/InMemoryLdap)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/3c64274de3ad4a13a3781956532ec08e)](https://www.codacy.com/manual/FlakM/InMemoryLdap?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=FlakM/InMemoryLdap&amp;utm_campaign=Badge_Grade)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.flakm/inmemoryldap_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.flakm/inmemoryldap_2.12)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Library that helps with in-memory testing against ldap using [UnboundID LDAP SDK for Java](https://ldap.com/unboundid-ldap-sdk-for-java/).

Inspired by [embedded-kafka](https://github.com/embeddedkafka/embedded-kafka)

## Version capability

`inmemoryldap` is available in MavenCentral compiled for scala 2.12 and 2.13.


## How to use

By default configuration for mock will be taken from [reference.conf](src/main/resources/reference.conf).
Library will look for ldif definitions on classpath `/ldap/test-data.ldif`.
Multiple files can be scheduled by manipulating `inmemoryldap.files` config property.
Ldif files will be loaded to server in order of occurrence before running any user code. 
There are two alternatives of running embedded ldap:

```scala
  import InMemoryLdapServer._
  withRunningLdap { implicit ds =>
    // ldap will be accessible here on default address: 127.0.0.1:1234
  }
  // before going further all resources will be pruned
```

On the other hand you might want to use your mocked ldap for longer periods of time (ie with trait `BeforeAndAfterAll`).
To do so you might use: 

```scala
    val server: InMemoryDirectoryServer = InMemoryLdapServer.start()
    // some code goes here  
    InMemoryLdapServer.stop()
```


## Customizing behaviour

Each method might be parameterized by optional configuration. To do so either override reference.conf using standard code from https://github.com/lightbend/config.
Alternatively both `com.github.flakm.InMemoryLdapServer.start` and `com.github.flakm.InMemoryLdapServer.withRunningLdapConfig` take optional config:

```scala
 withRunningLdapConfig(customConfig) { ds =>
  //code here
 }
```

For more examples read [here](src/test/scala/com/github/flakm/WithRunningLdapTest.scala) or [here](src/test/scala/com/github/flakm/StartStopTest.scala)

