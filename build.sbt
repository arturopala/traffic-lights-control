
organization := "com.github.arturopala"

name := "traffic-lights-control"

version := "0.1.0-SNAPSHOT"

resolvers += Resolver.mavenLocal

scalaVersion := "2.12.8"

val akkaVersion = "2.5.22"
val akkaHttpVersion = "10.1.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.softwaremill.macwire" %% "macros" % "2.3.2" % "provided",
  "com.softwaremill.macwire" %% "util" % "2.3.2",
  "org.slf4j" % "slf4j-api" % "1.7.26",
  "org.slf4j" % "slf4j-simple" % "1.7.26",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "org.scalatest" %% "scalatest" % "3.0.7" % Test,
  "org.scalacheck" %% "scalacheck" % "1.14.0" % Test
).map(_.withSources())

Revolver.settings

mainClass in (Compile, run) := Some("trafficlightscontrol.Boot")

lazy val root = (project in file("."))

fork := true

connectInput in run := true

outputStrategy := Some(StdoutOutput)

scalafmtOnCompile in Compile := true
scalafmtOnCompile in Test := true

