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

import models.EoriActionEnum
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
import views.html.{EoriActionView, EoriOperationSuccessfulView}

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
  private val eoriOpView = app.injector.instanceOf[EoriOperationSuccessfulView]

  private val controller = EoriActionController(mcc, view,eoriOpView, testAuthAction)

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

  "Show Page On Success" should {
    "return HTML for update success" in withSignedInUser {
      val oldEoriNumber = "GB123456789011"
      val newEoriNumber = "GB123456789012"
      val result = controller.showPageOnSuccess(Some(EoriActionEnum.UPDATE_EORI.toString), Some(oldEoriNumber), Some(newEoriNumber), Some(newEoriNumber), None,None,None)(fakeRequest)
      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
      contentAsString(result) should include(s"EORI number $oldEoriNumber has been replaced with $newEoriNumber")
    }

    "return HTML for cancel success" in withSignedInUser {
      val eoriNumber = "GB123456789011"
      val result = controller.showPageOnSuccess(Some(EoriActionEnum.CANCEL_EORI.toString), Some(eoriNumber), None,None,None,Some("HMRC-GVMS-ORG"),Some("HMRC-CTC-ORG"))(fakeRequest)
      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
      contentAsString(result) should include(s"Subscriptions cancelled for $eoriNumber")
    }

    "redirect to STRIDE login for not logged-in user" in withNotSignedInUser {
      val result = controller.showPageOnSuccess(Some(EoriActionEnum.UPDATE_EORI.toString), Some("GB123456789011"), Some("GB123456789012"), None,None, None,None)(fakeRequest)
      status(result) shouldBe SEE_OTHER
      val Some(redirectURL) = redirectLocation(result)
      redirectURL should include("/stride/sign-in")
    }
  }

  "Continue action" should {
    "redirect to update page when user select update Eori number" in withSignedInUser {
      val fakeRequestWithBody = FakeRequest("POST", "/")
        .withFormUrlEncodedBody("update-or-cancel-eori" -> EoriActionEnum.UPDATE_EORI.toString)
      val result = controller.continueAction(fakeRequestWithBody)
      status(result) shouldBe SEE_OTHER
      val Some(redirectURL) = redirectLocation(result)
      redirectURL should include("/update")
    }

    "redirect to cancel page when user select update Eori number" in withSignedInUser {
      val fakeRequestWithBody = FakeRequest("POST", "/")
        .withFormUrlEncodedBody("update-or-cancel-eori" -> EoriActionEnum.CANCEL_EORI.toString)
      val result = controller.continueAction(fakeRequestWithBody)
      status(result) shouldBe SEE_OTHER
      val Some(redirectURL) = redirectLocation(result)
      redirectURL should include("/cancel")
    }

    "redirect to STRIDE login for not logged-in user" in withNotSignedInUser {
      val result = controller.showPage(fakeRequest)
      status(result) shouldBe SEE_OTHER
      val Some(redirectURL) = redirectLocation(result)
      redirectURL should include("/stride/sign-in")
    }
  }
}
