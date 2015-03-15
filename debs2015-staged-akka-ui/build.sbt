name := """debs2015-staged-akka-ui"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "samzrat" %% "debs2015-staged-akka" % "1.0-SNAPSHOT"
)

EclipseKeys.createSrc := EclipseCreateSrc.All
