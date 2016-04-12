name := """bobbit"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

resolvers += "ReactiveCouchbase Releases" at "https://raw.github.com/ReactiveCouchbase/repository/master/releases/"
resolvers += Resolver.bintrayRepo("hmrc", "releases")


libraryDependencies ++= Seq(
  "io.netty" % "netty" % "3.9.2.Final",
  "org.reactivecouchbase" %% "reactivecouchbase-play" % "0.3",
  "org.scalatest" %% "scalatest" % "2.2.6",
  jdbc,
  cache,
  ws,
  specs2 % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
