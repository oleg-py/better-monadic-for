lazy val scala213 = "2.13.0-RC1"
lazy val scala212 = "2.12.8"
lazy val scala211 = "2.11.12"
lazy val supportedScalaVersions = List(scala211, scala212, scala213)

ThisBuild / organization := "com.olegpy"
ThisBuild / version := "0.2.4"
ThisBuild / licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
ThisBuild / homepage := Some(url("http://github.com/oleg-py/better-monadic-for"))
ThisBuild / scalaVersion := scala212

val testSettings = Seq(
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.8-RC2" % Test
  ),
  Test / scalacOptions ++= {
    val jar = (plugin / Compile / packageBin).value
    Seq(s"-Xplugin:${jar.getAbsolutePath}", s"-Jdummy=${jar.lastModified}") // ensures recompile
  },
  Test / scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) => Seq.empty
      case _ => Seq("-Ywarn-unused:locals")
    }
  },
  publish / skip := true
)

lazy val root = (project in file("."))
  .aggregate(plugin, pluginTests, catsTests, pcplodTests)
  .settings(
    crossScalaVersions := Nil,
    publish / skip := true
  )

lazy val plugin = (project in file("plugin"))
  .settings(
    name := "better-monadic-for",
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies ++= Seq(
      scalaOrganization.value % "scala-compiler" % scalaVersion.value,
    )
  )

lazy val pluginTests = (project in file("plugin-tests"))
  .dependsOn(plugin)
  .settings(crossScalaVersions := supportedScalaVersions)
  .settings(testSettings)

lazy val pcplodTests = (project in file("pcplod-tests"))
  .dependsOn(pluginTests % "compile->compile;test->test")
  .settings(
    name := "pcplod-tests",
    crossScalaVersions := List(scala211, scala212),
    libraryDependencies ++= Seq(
      "org.ensime" %% "pcplod" % "1.2.1" % Test
    ),
    // WORKAROUND https://github.com/ensime/pcplod/issues/12
    fork in Test := true,
    javaOptions in Test ++= Seq(
      s"""-Dpcplod.settings=${(scalacOptions in Test).value.filterNot(_.contains(",")).mkString(",")}""",
      s"""-Dpcplod.classpath=${(fullClasspath in Test).value.map(_.data).mkString(",")}"""
    )
  )
  .settings(testSettings)

lazy val catsTests = (project in file("cats-tests"))
  .dependsOn(pluginTests % "compile->compile;test->test")
  .settings(
    name := "cats-tests",
    crossScalaVersions := List(scala211, scala212),
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "1.6.0" % Test
    )
  )
  .settings(testSettings)
