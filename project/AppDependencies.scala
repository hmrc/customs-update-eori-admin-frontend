import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  private val bootstrapVersion = "7.12.0"
  

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-28" % bootstrapVersion,
    "uk.gov.hmrc"             %% "play-frontend-hmrc"         % "3.34.0-play-28"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % bootstrapVersion            % "test, it",
    "org.jsoup"               %  "jsoup"                      % "1.15.3"                    % Test,
    "org.scalatest"           %% "scalatest"                  % "3.2.14"                    % Test,
    "org.scalatestplus"       %% "mockito-4-6"                % "3.2.14.0"                  % Test,
    "org.scalatestplus.play"  %% "scalatestplus-play"         % "5.1.0"                     % Test,
    "com.vladsch.flexmark"    % "flexmark-all"                % "0.62.2"                    % Test
  )
}
