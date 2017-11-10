package io.leonard

import java.time.{OffsetDateTime, ZoneOffset}

import io.circe.java8.time._
import io.circe.generic.auto._
import io.circe.syntax._
import java.nio.file.{Files, Paths}
import java.nio.charset.StandardCharsets

import html.index

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

    val report = Report(commandRunner.projectName, now, results)
    report.writeJson()


    val htmlReport = index(report)
  }

}

case class Report(projectName: String, time: OffsetDateTime, results: Seq[TaskResult]) {

  def fileName = s"reports/$projectName-$time.json"

  lazy val sbtVersions: Set[SbtVersion] = results.flatMap(_.sbtVersions).toSet

  def writeJson(): Unit = Files.write(Paths.get(fileName), this.asJson.toString().getBytes(StandardCharsets.UTF_8))
  def writeHtml(): Unit = {
    val renderedHtml = html.index(this).body
    Files.write(Paths.get("reports/index.html"), renderedHtml.getBytes(StandardCharsets.UTF_8))
  }


}

case class SbtVersionResult(sbtVersion: SbtVersion, durationMillis: Long, log: String)
case class TaskResult(id: String,
                      command: Seq[String],
                      description: String,
                      sbtVersions: Seq[SbtVersion],
                      commands: Map[TaskId, SbtVersionResult])

case class SbtProject(projectName: String) {

  val sbtVersions = Seq("0.13.16", "1.0.3")

  def run(id: TaskId, command: Seq[String], description: String): TaskResult = {

    val results = sbtVersions.map { sbtVersion =>
      val prefixedCommand = Seq("sbt", "-sbt-version", sbtVersion) ++ command

      println("")
      println(s"Running command '${prefixedCommand.mkString(" ")}', description: '$description'")

      val before = System.currentTimeMillis()
      val log = sys.process
        .Process(prefixedCommand, new java.io.File(s"test-projects/$projectName"))
        .!!

      val after = System.currentTimeMillis()

      val duration = after - before
      println(s"Running command '${command.mkString(" ")}' took $duration ms")
      SbtVersionResult(sbtVersion, duration, log)
    }

    val resultsMap = results.groupBy(_.sbtVersion).mapValues(_.head)
    TaskResult(id, command, description, sbtVersions, resultsMap)
  }

}
