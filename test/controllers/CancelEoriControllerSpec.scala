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

package controllers

import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status
import play.api.http.Status.SEE_OTHER
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers.{charset, contentType, defaultAwaitTimeout, redirectLocation, status}
import service.EnrolmentService
import views.html.{CancelEoriProblemView, CancelEoriView, ConfirmCancelEoriView}

import scala.concurrent.ExecutionContext.Implicits.global

class CancelEoriControllerSpec extends AnyWordSpec
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

  private val fakeRequest = FakeRequest("GET", "/")
  private val mcc = app.injector.instanceOf[MessagesControllerComponents]
  private val viewCancelEori = app.injector.instanceOf[CancelEoriView]
  private val viewConfirmCancel = app.injector.instanceOf[ConfirmCancelEoriView]
  private val viewCancelEoriProblem = app.injector.instanceOf[CancelEoriProblemView]
  private val enrolmentService = app.injector.instanceOf[EnrolmentService]
  private val controller = CancelEoriController(mcc, viewCancelEori, viewConfirmCancel, viewCancelEoriProblem, testAuthAction, enrolmentService)

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
  }

  "GET /" should {
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

  "Continue cancel EORI" should {
    "redirect to show confirmation page when user click continue" in withSignedInUser {
      val fakeRequestWithBody = FakeRequest("POST", "/")
        .withFormUrlEncodedBody("existingEori" -> "GB94449442349", "dateOfEstablishment" -> "04/11/1987")
      val result = controller.continueCancelEori(fakeRequestWithBody)
      status(result) shouldBe Status.OK
    }

    "redirect to STRIDE login for not logged-in user" in withNotSignedInUser {
      val result = controller.showPage(fakeRequest)
      status(result) shouldBe SEE_OTHER
      val Some(redirectURL) = redirectLocation(result)
      redirectURL should include("/stride/sign-in")
    }
  }

  "Confirm cancel EORI" should {
    "redirect to show confirmation page when user click continue" in withSignedInUser {
      val fakeRequestWithBody = FakeRequest("POST", "/")
        .withFormUrlEncodedBody("existingEori" -> "GB123456789123", "dateOfEstablishment" -> "11/12/1997")
      val result = controller.confirmCancelEori(fakeRequestWithBody)
      status(result) shouldBe Status.SEE_OTHER
    }

    "redirect to STRIDE login for not logged-in user" in withNotSignedInUser {
      val result = controller.showPage(fakeRequest)
      status(result) shouldBe SEE_OTHER
      val Some(redirectURL) = redirectLocation(result)
      redirectURL should include("/stride/sign-in")
    }

  }

}

