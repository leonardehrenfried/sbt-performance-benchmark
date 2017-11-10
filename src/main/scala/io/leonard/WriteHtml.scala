package io.leonard

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import scala.io.Source
import io.circe.java8.time._
import io.circe._
import io.circe.parser._
import io.circe.generic.auto._

object WriteHtml extends App {

  override def main(args: Array[String]): Unit = {
    val lines = Source.fromFile("reports/large-classpath-2017-11-10T21:48:42.009Z.json").mkString

    val Right(json) = parse(lines)
    val report = json.as[Report].getOrElse(???)

    val renderedHtml = html.index(report).body

    Files.write(Paths.get("reports/index.html"), renderedHtml.getBytes(StandardCharsets.UTF_8))
  }

}
