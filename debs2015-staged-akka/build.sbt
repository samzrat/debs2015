name := "debs2015-staged-akka"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.1"

organization := "samzrat"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.6",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.6" % "test",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  "com.typesafe.play" %% "play" % "2.3.8")
  