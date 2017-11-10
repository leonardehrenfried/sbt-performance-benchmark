package io.leonard

import java.time.{OffsetDateTime, ZoneOffset}

import io.circe.java8.time._
import io.circe.generic.auto._
import io.circe.syntax._
import java.nio.file.{Paths, Files}
import java.nio.charset.StandardCharsets

object Hello extends App {

  override def main(args: Array[String]): Unit = {
    val commandRunner = SbtProject("large-classpath")

    val now = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)

    val update =
      commandRunner.run(Seq("sbt", "update"), "Prefill artifact cache")
    val compile = commandRunner.run(Seq("sbt", "cpl"), "Compile project once")
    val repeatedClassPathHashing = commandRunner.run(
      Seq("sbt", "'; cpl; cpl'"),
      "Compile project twice, testing repeated hashing of an unchanged classpath")

    val report = Report(now, Seq(update, compile, repeatedClassPathHashing))
    report.writeToFile
  }

}

case class Report(time: OffsetDateTime, commands: Seq[RunResult]) {
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
}
