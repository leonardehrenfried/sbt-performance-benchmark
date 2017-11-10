// Copyright: 2017 https://github.com/cakesolutions/sbt-cake/graphs
// License: http://www.apache.org/licenses/LICENSE-2.0
package net.cakesolutions
// Copyright 2015 - 2016 Sam Halliday (derived from sbt-sensible)
// License: http://www.apache.org/licenses/LICENSE-2.0

import sbt._
import sbt.Keys._
import play.core.PlayVersion
import wartremover._

object CakePlatformKeys {

  /** Convenient bundles for depending on platform / core libraries */
  object deps {
    val logback = Seq(
      "ch.qos.logback" % "logback-classic" % "1.2.3"
    ) ++ Seq(
      "org.slf4j" % "log4j-over-slf4j",
      "org.slf4j" % "slf4j-api",
      "org.slf4j" % "jul-to-slf4j",
      "org.slf4j" % "jcl-over-slf4j"
    ).map(_ % "1.7.25")

    def testing(config: Configuration) =
      Seq(
        // janino 3.0.6 is not compatible and causes http://www.slf4j.org/codes.html#replay
        "org.codehaus.janino" % "janino" % "2.7.8" % config,
        "org.scalatest" %% "scalatest" % "3.0.4" % config,
        "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % "1.1.6" % config,
        "org.scalacheck" %% "scalacheck" % "1.13.5" % config
      ) ++ logback.map(_ % config)
  }

  implicit class PlayOps(p: Project) {
    import play.sbt._
    import PlayImport.PlayKeys
    import play.twirl.sbt.Import.TwirlKeys

    // for consistency we prefer default SBT style layout
    // https://www.playframework.com/documentation/2.5.x/Anatomy
    def enablePlay: Project =
      p.enablePlugins(PlayScala)
        .disablePlugins(PlayLayoutPlugin)
        .settings(
          // false positives in generated code
          scalacOptions -= "-Ywarn-unused-import",
          // lots of warts in generated code
          wartremoverExcluded in Compile ++= routes.RoutesKeys.routes
            .in(Compile)
            .value,
          PlayKeys.playMonitoredFiles ++= (sourceDirectories in (Compile, TwirlKeys.compileTemplates)).value
        )
  }

}

object CakePlatformPlugin extends AutoPlugin {
  override def requires = CakeStandardsPlugin
  override def trigger = allRequirements

  val autoImport = CakePlatformKeys
  import autoImport._

  override val buildSettings = Seq()

  override val projectSettings = Seq(
    // logging should be available everywhere (opt out if you really must...)
    libraryDependencies ++= deps.logback,
    libraryDependencies += "com.typesafe" % "config" % "1.3.1",
    libraryDependencies ++= deps.testing(Test)
  )

}
