ThisBuild / scalaVersion := "2.13.10"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "whatcheer"
ThisBuild / organizationName := "whatcheer"

name := "netop"
scalacOptions += "-deprecation"

scalacOptions += "-unchecked"

resolvers += "jitpack.io" at "https://jitpack.io"

autoCompilerPlugins := true

libraryDependencies += "javax.servlet" % "javax.servlet-api" % "3.0.1" % "provided"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3" % "compile"

libraryDependencies += "net.liftweb" %% "lift-mapper" % "3.5.0" % "compile"

libraryDependencies += "net.liftweb" %% "lift-testkit" % "3.5.0" % "compile"

libraryDependencies += "org.scalameta" %% "munit" % "0.7.29"

libraryDependencies += "com.augustcellars.cose" % "cose-java" % "1.1.0"

libraryDependencies += "com.github.ipld" % "java-cid" % "1.3.7"

libraryDependencies += "com.h2database" % "h2" % "2.1.214"

libraryDependencies += "com.github.jsonld-java" % "jsonld-java" % "0.13.4"

enablePlugins(JettyPlugin)


