organization := "hackaton"

name := "traffic-lights-control"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.4"

resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.+" withSources(),
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.+" withSources(),
  "com.typesafe.akka" %% "akka-testkit" % "2.3.+" % Test withSources(),
  "org.scalatest" %% "scalatest" % "2.+" % Test withSources(),
  "junit" % "junit" % "4.+" % Test,
  "com.novocode" % "junit-interface" % "0.10" % Test,
  "org.slf4j" % "slf4j-api" % "1.7.+",
  "org.slf4j" % "slf4j-simple" % "1.7.+"
)

com.typesafe.sbt.SbtScalariform.scalariformSettings

mainClass in (Compile, run) := Some("SimpleApp")

