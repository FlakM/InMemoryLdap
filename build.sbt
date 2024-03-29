
inThisBuild(List(
  organization := "com.github.flakm",
  homepage := Some(url("https://github.com/FlakM/InMemoryLdap")),
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  developers := List(
    Developer(
      "FlakM",
      "Maciej Flak",
      "maciej.jan.flak@gmail.com",
      url("https://flakm.github.io/")
    )
  )
))

parallelExecution in ThisBuild := false

lazy val commonSettings = Seq(
  organization := "com.github.flakm",
  //  https://github.com/scoverage/sbt-scoverage/issues/295
  //  https://github.com/scala/bug/issues/11608
  //  todo after #295 being fixed we can bump jdk11 -> 13 and scala version to 2.13.1 (travis and here)
  scalaVersion := "2.13.0",
  crossScalaVersions := Seq("2.12.14", "2.13.6"),
  homepage := Some(url("https://github.com/FlakM/InMemoryLdap")),
  parallelExecution in Test := false,
  logBuffered in Test := false,
  fork in Test := true,
  javaOptions ++= Seq("-Xms512m", "-Xmx2048m"),
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8"),
  scalafmtOnCompile := true
)

val unboundidLdapsdkVersion = "6.0.0"
val typesafeConfigVersion = "1.4.1"
lazy val commonLibrarySettings = libraryDependencies ++= Seq(
  "com.unboundid" % "unboundid-ldapsdk" % unboundidLdapsdkVersion,
  "com.typesafe" % "config" % typesafeConfigVersion,
  "com.typesafe" %% "ssl-config-core" % "0.4.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "org.scalactic" %% "scalactic" % "3.0.8" % "test",
  "org.scalatest" %% "scalatest" % "3.0.8" % "test",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "test"
)


lazy val root = (project in file("."))
  .settings(name := "inmemoryldap")
  .settings(commonSettings: _*)
  .settings(commonLibrarySettings)

ThisBuild / scalafixDependencies += "org.scala-lang.modules" %% "scala-collection-migrations" % "2.4.4"
libraryDependencies +=  "org.scala-lang.modules" %% "scala-collection-compat" % "2.4.4"
addCompilerPlugin(scalafixSemanticdb)
scalacOptions ++= List("-Yrangepos", "-P:semanticdb:synthetics:on")

