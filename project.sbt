
organization in Global := "mesosphere.marathon"

name in Global := "example-plugins"

scalaVersion in Global := "2.11.11"

resolvers += Resolver.sonatypeRepo("releases")

lazy val plugins = project.in(file(".")).dependsOn(env)
lazy val env = project

packAutoSettings

packExcludeJars := Seq("scala-.*\\.jar")
