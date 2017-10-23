name := "env-plugin"

scalaVersion := "2.11.11"

version := "1.0"

resolvers += Resolver.sonatypeRepo("releases")

resolvers += "Mesosphere Public Repo" at "http://downloads.mesosphere.io/maven"

packAutoSettings

packExcludeJars := Seq("scala-.*\\.jar")

libraryDependencies ++= Seq(
  "mesosphere.marathon" %% "plugin-interface" % "1.3.5" % "provided",
  "org.slf4j" % "slf4j-api" % "1.7.12" % "provided",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)
