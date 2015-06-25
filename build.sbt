name := "NSAPI"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  specs2 % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

// my own dependencies
libraryDependencies ++= Seq(
  "com.netaporter" %% "scala-uri" % "0.4.6",
  "org.scalaj" %% "scalaj-http" % "1.1.4",
  "org.reactivemongo" %% "reactivemongo" % "0.10.5.0.akka23",
  "com.notnoop.apns" % "apns" % "0.1.6",
  "org.apache.commons" % "commons-lang3" % "3.4"
)

fork in run := true

parallelExecution in Test := false