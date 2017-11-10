package io.leonard

import java.time.{OffsetDateTime, ZoneOffset}

import io.circe.java8.time._
import io.circe.generic.auto._
import io.circe.syntax._
import java.nio.file.{Files, Paths}
import java.nio.charset.StandardCharsets

object Benchmark extends App {

  override def main(args: Array[String]): Unit = {
    val commandRunner = SbtProject("large-classpath")

    val now = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)

    val results = Seq(
      commandRunner.run("prefillCache", Seq("cpl"), "Prefill artifact cache"),
      commandRunner.run("startup", Seq("sbtVersion"), "Test sbt startup"),
      commandRunner.run("compile", Seq("cpl"), "Compile project once, nothing to compile"),
      commandRunner.run(
        "compileTwice",
        Seq(";cpl;cpl"), // sys.process is adding quotes so none here
        "Compile project twice (nothing to compile), testing repeated hashing of an unchanged classpath"
      )
    )

    val map = results.flatten.groupBy(_.sbtVersion)
    val report =
      Report(commandRunner.projectName, now, map)
    report.writeToFile()
  }

}

case class Report(projectName: String, time: OffsetDateTime, result: Map[String, Seq[RunResult]]) {

  def fileName = s"reports/$projectName-$time.json"

  def writeToFile(): Unit = {
    Files.write(Paths.get(fileName), this.asJson.toString().getBytes(StandardCharsets.UTF_8))
  }
}

case class RunResult(id: String,
                     command: Seq[String],
                     description: String,
                     sbtVersion: String,
                     durationMillis: Long,
                     log: String)

case class SbtProject(projectName: String) {

  val sbtVersions = Seq("0.13.16", "1.0.3")

  def run(id: String, command: Seq[String], description: String): Seq[RunResult] = {

    sbtVersions.map { version =>
      val prefixedCommand = Seq("sbt", "-sbt-version", version) ++ command
      println("")
      println(s"Running command '${prefixedCommand.mkString(" ")}', description: '$description'")

      val before = System.currentTimeMillis()
      val log = sys.process
        .Process(prefixedCommand, new java.io.File(s"test-projects/$projectName"))
        .!!

      val after = System.currentTimeMillis()

      val duration = after - before
      println(s"Running command '${command.mkString(" ")}' took $duration ms")
      RunResult(id, command, description, version, duration, log)
    }
  }

}
