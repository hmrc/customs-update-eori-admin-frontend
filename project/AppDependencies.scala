import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  private val bootstrapVersion = "7.12.0"
  

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-28" % bootstrapVersion,
    "uk.gov.hmrc"             %% "play-frontend-hmrc"         % "3.34.0-play-28",
    "org.typelevel"           %% "cats-core"                   % "2.9.0"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % bootstrapVersion          % "test, it",
    "org.jsoup"               %  "jsoup"                      % "1.13.1"                  % Test,
    "org.pegdown"             %  "pegdown"                     % "1.6.0"                   % Test,
    "org.mockito"             %  "mockito-core"               % "3.10.0"                  % Test,
    "org.scalatestplus.play"  %% "scalatestplus-play"         % "5.0.0"                   % Test,
    "org.scalatestplus"       %% "scalatestplus-mockito"      % "1.0.0-M2"                % Test
  )
}
