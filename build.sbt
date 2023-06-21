ThisBuild / organization := "whatcheer"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.10"

lazy val commonSettings = Seq(
  organization := "whatcheer",
  organizationName := "whatcheer",
  scalacOptions += "-language:experimental.macros",
  scalacOptions += "-language:implicitConversions",
  scalacOptions += "-Ymacro-annotations",
  scalacOptions += "-deprecation"
)

name := "netop"

lazy val scalaReflect = Def.setting {
  "org.scala-lang" % "scala-reflect" % scalaVersion.value
}

autoCompilerPlugins := true

lazy val core = (project in file("core"))
  .dependsOn(macroSub)
  .settings(
    commonSettings,
    scalacOptions += "-deprecation",
    scalacOptions += "-feature",
    resolvers += "jitpack.io" at "https://jitpack.io",
    libraryDependencies += "javax.servlet" % "javax.servlet-api" % "3.0.1" % "provided",
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3" % "compile",
    libraryDependencies += "net.liftweb" %% "lift-mapper" % "3.5.0" % "compile",
    libraryDependencies += "net.liftweb" %% "lift-testkit" % "3.5.0" % "compile",
    libraryDependencies += "org.scalameta" %% "munit" % "0.7.29",
    libraryDependencies += "com.augustcellars.cose" % "cose-java" % "1.1.0",
    libraryDependencies += "com.github.ipld" % "java-cid" % "1.3.7",
    libraryDependencies += "com.h2database" % "h2" % "2.1.214",
    libraryDependencies += "org.postgresql" % "postgresql" % "42.6.0",
    libraryDependencies += "com.github.jsonld-java" % "jsonld-java" % "0.13.4",
    // Customize the Jetty port based on the contents of `localdev.props`
// Note that this file is in `.gitignore` so it won't be checked into
// the source code repo. Thus, one can designate a port that's reverse proxied
// from a public https server and it goes to a port that's only used for
// this app and is hidden from the public
    containerPort := {
      try {
        val props = (new java.util.Properties())
        props.load(new java.io.FileInputStream("localdev.props"))
        val port =
          java.lang.Integer
            .parseInt(props.get("jetty.port").asInstanceOf[String])
        port
      } catch {
        case _: Throwable => 8080
      }
    }
  )
  .enablePlugins(JettyPlugin)

lazy val macroSub = (project in file("macro"))
  .settings(
    commonSettings,
    libraryDependencies += scalaReflect.value,
    libraryDependencies += "net.liftweb" %% "lift-json" % "3.5.0" % "compile"
    // other settings here
  )
