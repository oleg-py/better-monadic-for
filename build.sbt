name := "better-monadic-for"
organization := "com.olegpy"
version := "0.3.0-M4"
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
homepage := Some(url("http://github.com/oleg-py/better-monadic-for"))

scalaVersion := "2.12.8"
crossScalaVersions := Seq("2.11.12", "2.12.8", "2.13.0-M5")
libraryDependencies ++= Seq(
  scalaOrganization.value % "scala-compiler" % scalaVersion.value,
  "org.scalatest" %% "scalatest" % "3.0.6-SNAP5" % Test,
  "org.scalaz" %% "scalaz-core" % "7.3.0-M26" % Test,
)

libraryDependencies ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 13)) => "org.typelevel" % "cats-effect_2.13.0-M4" % "1.1.0-M1" ::
      Nil
    case _ =>
      "org.typelevel" %% "cats-effect" % "1.0.0" % Test ::
      "org.ensime" %% "pcplod" % "1.2.1" % Test ::
      Nil
  }
}
// WORKAROUND https://github.com/ensime/pcplod/issues/12
fork in Test := true

javaOptions in Test ++= Seq(
  s"""-Dpcplod.settings=${(scalacOptions in Test).value.filterNot(_.contains(",")).mkString(",")}""",
  s"""-Dpcplod.classpath=${(fullClasspath in Test).value.map(_.data).mkString(",")}"""
)

scalacOptions in Test ++= {
  val jar = (packageBin in Compile).value
  Seq(s"-Xplugin:${jar.getAbsolutePath}", s"-Jdummy=${jar.lastModified}") // ensures recompile
}


//scalacOptions in Test += "-Xfatal-warnings"
scalacOptions in Test ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, x)) if x >= 12 => "-Ywarn-unused:locals" :: "-Ywarn-unused:params" :: Nil
    case _ => Nil
  }
}
