import io.gatling.sbt.GatlingPlugin
import io.gatling.sbt.GatlingPlugin._
import scalariform.formatter.preferences._

organization := "hackaton"

name := "traffic-lights-control"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.6"

resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.9",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.9",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.9" % Test,
  "org.scalatest" %% "scalatest" % "2.+" % Test,
  "junit" % "junit" % "4.12" % Test,
  "com.novocode" % "junit-interface" % "0.10" % Test,
  "org.slf4j" % "slf4j-api" % "1.7.10",
  "org.slf4j" % "slf4j-simple" % "1.7.10",
  "io.spray"  %%  "spray-can"     % "1.3.2",
  "io.spray"  %%  "spray-routing" % "1.3.2",
  "io.spray"  %%  "spray-json"    % "1.3.1",
  "io.spray"  %%  "spray-testkit"    % "1.3.2" % Test,
  "com.softwaremill.macwire" %% "macros" % "0.8.0",
  "com.wandoulabs.akka" %% "spray-websocket" % "0.1.4" excludeAll(
    ExclusionRule(organization = "io.spray")
  )
).map(_.withSources())

scoverage.ScoverageSbtPlugin.instrumentSettings

com.typesafe.sbt.SbtScalariform.scalariformSettings

ScalariformKeys.preferences := PreferencesImporterExporter.loadPreferences(baseDirectory.value / "project" / "formatterPreferences.properties" toString)

Revolver.settings

EclipseKeys.skipParents in ThisBuild := false

mainClass in (Compile, run) := Some("trafficlightscontrol.Boot")

lazy val loadtester = (project in file("loadtester")).enablePlugins(GatlingPlugin)

lazy val root = (project in file(".")).enablePlugins(SbtTwirl)

fork := true

