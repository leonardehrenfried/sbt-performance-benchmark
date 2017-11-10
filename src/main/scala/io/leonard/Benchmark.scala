package io.leonard

import java.time.{OffsetDateTime, ZoneOffset}

import io.circe.java8.time._
import io.circe.generic.auto._
import io.circe.syntax._
import java.nio.file.{Files, Paths}
import java.nio.charset.StandardCharsets

import scala.io.Source

object Benchmark extends App {

  override def main(args: Array[String]): Unit = {
    val commandRunner = SbtProject("large-classpath")

    val now = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)

    val results = Seq(
      commandRunner.run(Seq("sbt", "update"), "Prefill artifact cache"),
      commandRunner.run(Seq("sbt", "sbtVersion"), "Test sbt startup"),
      commandRunner.run(Seq("sbt", "cpl"), "Compile project once"),
      commandRunner.run(
        Seq("sbt", ";cpl;cpl"), // sys.process is adding quotes so none here
        "Compile project twice, testing repeated hashing of an unchanged classpath")
    )

    val report =
      Report(commandRunner.projectName, commandRunner.sbtVersion, now, results)
    report.writeToFile
  }

}

case class Report(projectName: String,
                  sbtVersion: String,
                  time: OffsetDateTime,
                  commands: Seq[RunResult]) {

  def fileName = s"reports/$time.json"

  def writeToFile: Unit = {
    Files.write(Paths.get(fileName),
                this.asJson.toString().getBytes(StandardCharsets.UTF_8))
  }
}

case class RunResult(command: Seq[String],
                     description: String,
                     durationMillis: Long,
                     log: String)

case class SbtProject(projectName: String) {

  def run(command: Seq[String], description: String): RunResult = {
    println("")
    println(
      s"Running command '${command.mkString(" ")}', description: '$description'")

    val before = System.currentTimeMillis()
    val log = sys.process
      .Process(command, new java.io.File(s"test-projects/$projectName"))
      .!!

    val after = System.currentTimeMillis()

    val duration = after - before
    println(s"Running command '${command.mkString(" ")}' took $duration ms")
    RunResult(command, description, duration, log)
  }

  lazy val sbtVersion = {
    val x = Source
      .fromFile(s"test-projects/$projectName/project/build.properties")
      .mkString("")
      .trim
    x.split("=").last
  }
}
