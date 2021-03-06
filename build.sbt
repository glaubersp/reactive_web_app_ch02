name := """reactive_web_app_ch02"""
organization := "org.glauber"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.8"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.1" % Test
libraryDependencies += ws
libraryDependencies += "org.reactivemongo" %% "play2-reactivemongo" % "0.16.0-play26"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "org.glauber.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "org.glauber.binders._"
