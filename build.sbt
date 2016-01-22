name := "reactive-hdlc"

version := "1.0"

organization := "pl.newit"

startYear := Some(2015)

scalaVersion := "2.11.7"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings", "-target:jvm-1.8")

libraryDependencies ++= Seq(
  "org.scalacheck"    %% "scalacheck"               % "1.12.5" % "test",
  "com.typesafe.akka" %% "akka-stream-experimental" % "2.0.1"
)