/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

import controllers.EoriActionController

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.inject.guice.GuiceApplicationBuilder

class EoriActionControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {
  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "metrics.jvm"     -> false,
        "metrics.enabled" -> false
      )
      .build()

  private val fakeRequest = FakeRequest("GET", "/")

  private val controller = app.injector.instanceOf[EoriActionController]

  "GET /" should {
    "return 200" in {
      val result = controller.showPage(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return HTML" in {
      val result = controller.showPage(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result)     shouldBe Some("utf-8")
    }
  }
}
