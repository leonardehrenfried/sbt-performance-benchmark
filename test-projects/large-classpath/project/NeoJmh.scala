// Copyright: 2017 Sam Halliday
// License: http://www.gnu.org/licenses/lgpl.html
import sbt._
import sbt.Keys._

import net.cakesolutions._

object NeoJmhKeys {

  /** Where you put your JMH code. */
  val Jmh = config("jmh") extend Test

  /** The runner, tries to be hidden but needed for runtime settings */
  val JmhInternal = (config("jmh-internal") extend Test).hide

  val neoJmhGenerator = settingKey[String]("Available: `reflection` or `asm`")
}

/**
  * https://github.com/ktoso/sbt-jmh/ rewritten as an idiomatic sbt
  * Configuration (not requiring a separate Project).
  */
object NeoJmh extends AutoPlugin {
  import NeoJmhKeys._
  val autoImport = NeoJmhKeys

  val generateJmhSourcesAndResources = taskKey[(Seq[File], Seq[File])](
    "Generate benchmark JMH Java code and resources")

  override def requires = plugins.JvmPlugin
  override def trigger = noTrigger
  override def projectConfigurations = Seq(Jmh, JmhInternal)

  override def projectSettings =
    inConfig(Jmh)(
      Defaults.testSettings ++
        CakeBuildKeys.sensibleTestSettings ++ Seq(
        run := (run in JmhInternal).evaluated,
        neoJmhGenerator := "reflection"
      )
    ) ++ inConfig(JmhInternal)(
      Defaults.testSettings ++
        CakeBuildKeys.sensibleTestSettings ++ Seq(
        mainClass in run := Some("org.openjdk.jmh.Main"),
        fork in run := true,
        dependencyClasspath ++= (fullClasspath in Jmh).value,
        sourceGenerators += generateJmhSourcesAndResources.map {
          case (sources, _) => sources
        },
        resourceGenerators += generateJmhSourcesAndResources.map {
          case (_, res) => res
        },
        generateJmhSourcesAndResources := generateBenchmarkSourcesAndResources.value
      )
    ) ++ Seq(
      libraryDependencies ++= Seq(
        "jmh-core",
        "jmh-generator-bytecode",
        "jmh-generator-reflection",
        "jmh-generator-asm"
      ).map("org.openjdk.jmh" % _ % "1.19" % Jmh.name)
    )

  def generateBenchmarkSourcesAndResources
    : Def.Initialize[Task[(Seq[File], Seq[File])]] = Def.task {
    val s = streams.value
    val cacheDir = crossTarget.value / "jmh-cache"
    val bytecodeDir = (classDirectory in Jmh).value
    val sourceDir = sourceManaged.value
    val resourceDir = resourceManaged.value
    val generator = (neoJmhGenerator in Jmh).value
    val classpath = dependencyClasspath.value

    val inputs: Set[File] = (bytecodeDir ** "*").filter(_.isFile).get.toSet
    val cachedGeneration = FileFunction.cached(cacheDir, FilesInfo.hash) { _ =>
      IO.delete(sourceDir)
      IO.createDirectory(sourceDir)
      IO.delete(resourceDir)
      IO.createDirectory(resourceDir)

      val options = ForkOptions(
        javaHome = javaHome.value,
        outputStrategy = outputStrategy.value,
        bootJars = Vector.empty[java.io.File],
        workingDirectory = Option(baseDirectory.value),
        runJVMOptions = Vector.empty[String],
        connectInput = connectInput.value,
        envVars = Map.empty[String, String]
      )
      new ForkRun(options)
        .run(
          "org.openjdk.jmh.generators.bytecode.JmhBytecodeGenerator",
          Attributed.data(classpath),
          List(bytecodeDir.getPath,
               sourceDir.getPath,
               resourceDir.getPath,
               generator),
          s.log
        )
        .failed
        .foreach(f => sys.error(f.getMessage))

      ((sourceDir ** "*").filter(_.isFile) +++ (resourceDir ** "*").filter(
        _.isFile)).get.toSet
    }
    cachedGeneration(inputs).toSeq.partition(f =>
      IO.relativizeFile(sourceDir, f).nonEmpty)
  }

}
