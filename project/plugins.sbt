resolvers += Resolver.typesafeRepo("releases")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.12")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.2.1")

addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.4.31")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.0")

addSbtPlugin("com.codacy" % "sbt-codacy-coverage" % "3.0.3")