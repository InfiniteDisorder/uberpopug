name := "uberpopug"

version := "0.1"

scalaVersion := "2.13.9"

lazy val Version = new {
  val scala = "2.13.9"
  val http4s = "0.23.0"
  val cats = new {
    val core = "2.7.0"
    val effect = "3.3.11"
  }
  val circe = "0.14.1"
  val tapir = "1.0.0-M6"
}

lazy val scalaDeps = Seq(
  "org.scala-lang" % "scala-compiler" % Version.scala,
  "org.scala-lang" % "scala-reflect" % Version.scala
)

lazy val catsDeps = Seq(
  "org.typelevel" %% "cats-core" % Version.cats.core,
  "org.typelevel" %% "cats-kernel" % Version.cats.core,
  "org.typelevel" %% "cats-effect" % Version.cats.effect,
  "org.typelevel" %% "cats-effect-testing-scalatest" % "1.4.0" % Test,
  "org.typelevel" %% "mouse" % "1.0.11"
)

lazy val circeDeps = Seq(
  "io.circe" %% "circe-core" % Version.circe,
  "io.circe" %% "circe-parser" % Version.circe,
  "io.circe" %% "circe-literal" % Version.circe,
  "io.circe" %% "circe-refined" % Version.circe,
  "io.circe" %% "circe-derivation" % "0.13.0-M4"
)

lazy val http4sDeps = Seq(
  "org.http4s" %% "http4s-dsl" % Version.http4s,
  "org.http4s" %% "http4s-blaze-server" % Version.http4s,
  "org.http4s" %% "http4s-blaze-client" % Version.http4s,
  "org.http4s" %% "http4s-circe" % Version.http4s
)

lazy val tapirDeps = Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-core" % Version.tapir,
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % Version.tapir,
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % Version.tapir
)

libraryDependencies ++=
  (scalaDeps ++
    catsDeps ++
    circeDeps ++
    http4sDeps ++
    tapirDeps) ++
    Seq(
      "com.github.fd4s" %% "fs2-kafka" % "2.5.0-M3",
      "org.tpolecat" %% "doobie-core" % "1.0.0-RC2",
      "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC2",
      "org.slf4j" % "slf4j-nop" % "1.6.4"
    )

scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-unchecked",
  "-feature",
  "-deprecation",
  "-explaintypes",
  "-language:postfixOps",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:reflectiveCalls",
  "-language:existentials",
  "-Ymacro-annotations"
)
