name := "better-monadic-for"
organization := "com.olegpy"
version := "0.1"

scalaVersion := "2.12.5"
crossScalaVersions := Seq("2.11.12", "2.12.5")
libraryDependencies ++= Seq(
  scalaOrganization.value % "scala-compiler" % scalaVersion.value,
  "com.lihaoyi" %% "utest" % "0.5.3" % Test,
)

scalacOptions in Test ++= {
  val jar = (packageBin in Compile).value
  Seq(s"-Xplugin:${jar.getAbsolutePath}", s"-Jdummy=${jar.lastModified}") // ensures recompile
}
