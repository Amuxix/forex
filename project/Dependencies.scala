import sbt._

object Dependencies {

  object Versions {
    val cats       = "2.7.0"
    val catsEffect = "3.3.5"
    val fs2        = "3.2.5"
    val http4s     = "0.23.11"
    val circe      = "0.13.0"
    val pureConfig = "0.17.1"
    val scafeine   = "5.1.2"

    val kindProjector      = "0.13.2"
    val logback            = "1.2.11"
    val scalaCheck         = "1.16.0"
    val scalaTest          = "3.2.12"
    val scalaTestPlusCheck = "3.2.12.0"
    val catsScalaCheck     = "0.3.1"

    val organizeImports = "0.6.0"
  }

  object Libraries {
    def circe(artifact: String): ModuleID  = "io.circe"   %% artifact % Versions.circe
    def http4s(artifact: String): ModuleID = "org.http4s" %% artifact % Versions.http4s

    lazy val cats       = "org.typelevel" %% "cats-core"   % Versions.cats
    lazy val catsEffect = "org.typelevel" %% "cats-effect" % Versions.catsEffect
    lazy val fs2        = "co.fs2"        %% "fs2-core"    % Versions.fs2

    lazy val http4sDsl       = http4s("http4s-dsl")
    lazy val http4sServer    = http4s("http4s-blaze-server")
    lazy val http4sClient    = http4s("http4s-blaze-client")
    lazy val http4sCirce     = http4s("http4s-circe")
    lazy val circeCore       = circe("circe-core")
    lazy val circeGeneric    = circe("circe-generic")
    lazy val circeGenericExt = circe("circe-generic-extras")
    lazy val circeParser     = circe("circe-parser")
    lazy val pureConfig      = "com.github.pureconfig" %% "pureconfig" % Versions.pureConfig
    lazy val scafeine        = "com.github.blemale" %% "scaffeine" % Versions.scafeine

    // Runtime
    lazy val logback = "ch.qos.logback" % "logback-classic" % Versions.logback

    // Test
    lazy val scalaTest          = "org.scalatest"     %% "scalatest"       % Versions.scalaTest
    lazy val scalaTestPlusCheck = "org.scalatestplus" %% "scalacheck-1-16" % Versions.scalaTestPlusCheck
    lazy val scalaCheck         = "org.scalacheck"    %% "scalacheck"      % Versions.scalaCheck
  }

  object ScalaFix {
    val organizeImports = "com.github.liancheng" %% "organize-imports" % Versions.organizeImports
  }

}
