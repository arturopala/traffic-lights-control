organization := "hackaton"

name := "traffic-lights-control"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.4"

resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.+",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.+",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.+" % Test,
  "org.scalatest" %% "scalatest" % "2.+" % Test,
  "junit" % "junit" % "4.+" % Test,
  "com.novocode" % "junit-interface" % "0.10" % Test,
  "org.slf4j" % "slf4j-api" % "1.7.+",
  "org.slf4j" % "slf4j-simple" % "1.7.+",
  "io.spray"  %%  "spray-can"     % "1.3.2",
  "io.spray"  %%  "spray-routing" % "1.3.2",
  "io.spray"  %%  "spray-json"    % "1.3.0",
  "com.softwaremill.macwire" %% "macros" % "0.7.3",
  "com.wandoulabs.akka" %% "spray-websocket" % "0.1.3" excludeAll(
    ExclusionRule(organization = "io.spray")
  )
).map(_.withSources())

com.typesafe.sbt.SbtScalariform.scalariformSettings

Revolver.settings

scoverage.ScoverageSbtPlugin.instrumentSettings

mainClass in (Compile, run) := Some("trafficlightscontrol.Boot")

