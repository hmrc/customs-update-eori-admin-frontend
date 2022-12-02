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
    
    "org.jsoup"               %  "jsoup"                      % "1.13.1"            % Test,
  )
}
