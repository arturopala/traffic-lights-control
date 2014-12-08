import io.gatling.sbt.GatlingPlugin
import io.gatling.sbt.GatlingPlugin._

organization := "hackaton"

name := "traffic-lights-control-loadtester"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.10.4"

resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
  "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.0.3",
  "io.gatling" % "test-framework" % "1.0"
).map(_.withSources())

com.typesafe.sbt.SbtScalariform.scalariformSettings

scalaSource in Gatling <<= baseDirectory(_ / "src" / "main" / "scala")

resourceDirectory in Gatling <<= baseDirectory(_ / "src" / "main" / "resources")