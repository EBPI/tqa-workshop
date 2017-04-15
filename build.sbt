
name := "tqa-workshop"

organization := "eu.cdevreeze.tqaworkshop"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.8"

crossScalaVersions := Seq("2.11.8", "2.12.1")

// See: Toward a safer Scala
// http://downloads.typesafe.com/website/presentations/ScalaDaysSF2015/Toward%20a%20Safer%20Scala%20@%20Scala%20Days%20SF%202015.pdf

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings", "-Xlint")

(unmanagedSourceDirectories in Compile) <++= (scalaBinaryVersion, baseDirectory) apply { case (version, base) =>
  if (version.contains("2.12")) Seq(base / "src" / "main" / "scala-2.12")
  else if (version.contains("2.11")) Seq(base / "src" / "main" / "scala-2.11") else Seq()
}

(unmanagedSourceDirectories in Test) <++= (scalaBinaryVersion, baseDirectory) apply { case (version, base) =>
  if (version.contains("2.12")) Seq(base / "src" / "test" / "scala-2.12")
  else if (version.contains("2.11")) Seq(base / "src" / "test" / "scala-2.11") else Seq()
}

libraryDependencies += "eu.cdevreeze.tqa" %% "tqa" % "0.4.2" excludeAll(
  ExclusionRule(organization = "eu.cdevreeze.yaidom")
)

libraryDependencies += "eu.cdevreeze.yaidom" %% "yaidom" % "1.6.2"

libraryDependencies += "junit" % "junit" % "4.12" % "test"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"


// resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"

// addCompilerPlugin("com.artima.supersafe" %% "supersafe" % "1.0.3")

