import sbt.*

object AppDependencies {

  private val bootstrapVersion = "10.4.0"
  private val playVersion = "play-30"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"   %% s"bootstrap-frontend-$playVersion" % bootstrapVersion,
    "uk.gov.hmrc"   %% s"play-frontend-hmrc-$playVersion" % "12.21.0",
    "org.typelevel" %% "cats-core"                        % "2.13.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% s"bootstrap-test-$playVersion" % bootstrapVersion,
    "org.jsoup"               % "jsoup"                        % "1.21.2",
    "org.scalatestplus"      %% "mockito-4-11"                  % "3.2.18.0",
    "org.scalatestplus.play" %% "scalatestplus-play"           % "7.0.2"
  ).map(_ % Test)
}
