import sbt._

object AppDependencies {

  private val bootstrapVersion = "8.4.0"
  private val playVersion = "play-30"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"   %% s"bootstrap-frontend-$playVersion" % bootstrapVersion,
    "uk.gov.hmrc"   %% s"play-frontend-hmrc-$playVersion" % "9.1.0",
    "org.typelevel" %% "cats-core"                  % "2.10.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-test-$playVersion" % bootstrapVersion,
    "org.jsoup"               % "jsoup"                  % "1.17.2",
    "org.scalatestplus"      %% "mockito-4-6"            % "3.2.15.0",
    "org.scalatestplus.play" %% "scalatestplus-play"     % "5.1.0"
  ).map(_ % Test)
}
