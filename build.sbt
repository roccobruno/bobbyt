name := """bobbit"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

resolvers += "ReactiveCouchbase Releases" at "https://raw.github.com/ReactiveCouchbase/repository/master/releases/"
resolvers += Resolver.bintrayRepo("hmrc", "releases")
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/releases"
resolvers += DefaultMavenRepository

libraryDependencies ++= Seq(
  "io.netty" % "netty" % "3.9.2.Final",
  "org.reactivecouchbase" %% "reactivecouchbase-play" % "0.3",
  "com.couchbase.client" % "java-client" % "2.3.5",
  "async-couchbase" %% "async-couchbase" % "0.1-SNAPSHOT",
  "org.scalatest" %% "scalatest" % "2.2.6",
  "io.igl" %% "jwt" % "1.2.0",
  jdbc,
  cache,
  ws,
  specs2 % Test
)


resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
