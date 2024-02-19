import sbt._

object AppDependencies {

  private val bootstrapVersion = "8.4.0"

  val compile = Seq(
    "uk.gov.hmrc"   %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc"   %% "play-frontend-hmrc-play-30" % bootstrapVersion,
    "org.typelevel" %% "cats-core"                  % "2.10.0"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30" % bootstrapVersion,
    "org.jsoup"               % "jsoup"                  % "1.15.4",
    "org.scalatest"          %% "scalatest"              % "3.2.15",
    "org.scalatestplus"      %% "mockito-4-6"            % "3.2.15.0",
    "org.scalatestplus.play" %% "scalatestplus-play"     % "5.1.0"
  ).map(_ % Test)
}
