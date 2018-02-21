
name := "tqa-workshop"

organization := "eu.cdevreeze.tqaworkshop"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.11"

crossScalaVersions := Seq("2.11.11", "2.12.4")

// See: Toward a safer Scala
// http://downloads.typesafe.com/website/presentations/ScalaDaysSF2015/Toward%20a%20Safer%20Scala%20@%20Scala%20Days%20SF%202015.pdf

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xlint")
// No -Xfatal-warnings, because we often introduce unsed code in the tests

libraryDependencies += "eu.cdevreeze.tqa" %% "tqa" % "0.7.0" excludeAll(
  ExclusionRule(organization = "eu.cdevreeze.yaidom")
)

libraryDependencies += "eu.cdevreeze.yaidom" %% "yaidom" % "1.7.1"

libraryDependencies += "junit" % "junit" % "4.12" % "test"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4" % "test"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.13.5" % "test"


// resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"

// addCompilerPlugin("com.artima.supersafe" %% "supersafe" % "1.0.3")

