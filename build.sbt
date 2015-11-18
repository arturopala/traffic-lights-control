import scalariform.formatter.preferences._

organization := "me.arturopala"

name := "traffic-lights-control"

version := "0.1.0-SNAPSHOT"

resolvers += Resolver.mavenLocal

scalaVersion := "2.11.7"

val akkaVersion = "2.4.0"
val akkaHttpVersion = "2.0-M1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-experimental" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-core-experimental" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-experimental" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-xml-experimental" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaHttpVersion,
  "io.spray" %%  "spray-json" % "1.3.2",
  "com.softwaremill.macwire" %% "macros" % "2.1.0" % "provided",
  "com.softwaremill.macwire" %% "util" % "2.1.0",
  "org.slf4j" % "slf4j-api" % "1.7.12",
  "org.slf4j" % "slf4j-simple" % "1.7.12",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit-experimental" % akkaHttpVersion % Test,
  "com.typesafe.akka" %% "akka-http-testkit-experimental" % akkaHttpVersion % Test,
  "org.scalatest" %% "scalatest" % "2.2.5" % Test,
  "org.scalacheck" %% "scalacheck" % "1.12.5" % Test
).map(_.withSources())

com.typesafe.sbt.SbtScalariform.scalariformSettings

ScalariformKeys.preferences := PreferencesImporterExporter.loadPreferences(baseDirectory.value / "project" / "formatterPreferences.properties" toString)

Revolver.settings

EclipseKeys.skipParents in ThisBuild := false

mainClass in (Compile, run) := Some("trafficlightscontrol.Boot")

lazy val root = (project in file("."))

fork := true

connectInput in run := true

outputStrategy := Some(StdoutOutput)

