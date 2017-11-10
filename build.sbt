organization := "io.leonard"
scalaVersion := "2.12.4"
version      := "0.1.0-SNAPSHOT"

val circeVersion = "0.8.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-java8",
).map(_ % circeVersion)


