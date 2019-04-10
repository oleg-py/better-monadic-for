name := "better-monadic-for"
organization := "com.olegpy"
version := "0.2.4"
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
homepage := Some(url("http://github.com/oleg-py/better-monadic-for"))

scalaVersion := "2.12.5"
crossScalaVersions := Seq("2.11.12", "2.12.5", "2.13.0-RC1")
libraryDependencies ++= Seq(
  scalaOrganization.value % "scala-compiler" % scalaVersion.value,
  "org.scalatest" %% "scalatest" % "3.0.8-RC2" % Test
)
libraryDependencies ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, minor)) if minor <= 12 =>
      Seq("org.ensime" %% "pcplod" % "1.2.1" % Test)
    case _ =>
      Seq.empty
  }
}

unmanagedSourceDirectories in Test ++= {
  (unmanagedSourceDirectories in Test).value.flatMap { dir =>
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11 | 12)) => Seq(file(dir.getPath ++ "-2.11-2.12"))
      case Some((2, 13)) => Seq(file(dir.getPath ++ "-2.13"))
      case _ => Seq.empty
    }
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
    case Some((2, 11)) => Seq.empty
    case _ => Seq("-Ywarn-unused:locals")
  }
}
