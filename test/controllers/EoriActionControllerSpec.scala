/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import controllers.{AuthenticationBehaviours, EoriActionController}
import models.EoriAction
import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.EoriActionView

class EoriActionControllerSpec
  extends AnyWordSpec
    with Matchers
    with GuiceOneAppPerSuite
    with AuthenticationBehaviours
    with MockitoSugar
    with BeforeAndAfterEach {
  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "metrics.jvm" -> false,
        "metrics.enabled" -> false
      )
      .build()

  val fakeRequest = FakeRequest("GET", "/")

  private val mcc = app.injector.instanceOf[MessagesControllerComponents]
  private val view = app.injector.instanceOf[EoriActionView]

  private val controller = EoriActionController(mcc, view, testAuthAction)

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
  }

  "Show Page" should {
    "return 200" in withSignedInUser {
      val result = controller.showPage(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return HTML" in withSignedInUser {
      val result = controller.showPage(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    "redirect to STRIDE login for not logged-in user" in withNotSignedInUser {
      val result = controller.showPage(fakeRequest)
      status(result) shouldBe SEE_OTHER
      val Some(redirectURL) = redirectLocation(result)
      redirectURL should include("/stride/sign-in")
    }
  }

  "Continue action" should {
    "redirect to update page when user select update Eori number" in withSignedInUser {
      val fakeRequestWithBody = FakeRequest("POST", "/")
        .withFormUrlEncodedBody("update-or-cancel-eori" -> EoriAction.UPDATE_EORI.toString)
      val result = controller.continueAction(fakeRequestWithBody)
      status(result) shouldBe SEE_OTHER
      val Some(redirectURL) = redirectLocation(result)
      redirectURL should include("/update")
    }

    "redirect to cancel page when user select update Eori number" in withSignedInUser {
      val fakeRequestWithBody = FakeRequest("POST", "/")
        .withFormUrlEncodedBody("update-or-cancel-eori" -> EoriAction.CANCEL_EORI.toString)
      val result = controller.continueAction(fakeRequestWithBody)
      status(result) shouldBe SEE_OTHER
      val Some(redirectURL) = redirectLocation(result)
      redirectURL should include("/") // TODO fix that when we have cancel page
    }

    "redirect to STRIDE login for not logged-in user" in withNotSignedInUser {
      val result = controller.showPage(fakeRequest)
      status(result) shouldBe SEE_OTHER
      val Some(redirectURL) = redirectLocation(result)
      redirectURL should include("/stride/sign-in")
    }
  }
}
