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

import models.{Enrolment, EnrolmentKey, Eori, ValidateEori}
import models.LocalDateBinder.stringToLocalDate
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{reset, when}
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
import service.EnrolmentService
import views.html.{ConfirmEoriUpdateView, UpdateEoriProblemView, UpdateEoriView}

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UpdateEoriControllerSpec
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

  private val fakeRequest = FakeRequest("GET", "/")
  private val mcc = app.injector.instanceOf[MessagesControllerComponents]
  private val viewUpdateEori = app.injector.instanceOf[UpdateEoriView]
  private val viewConfirmUpdate = app.injector.instanceOf[ConfirmEoriUpdateView]
  private val viewEoriProblem = app.injector.instanceOf[UpdateEoriProblemView]
  private val enrolmentService = mock[EnrolmentService]
  private val controller = UpdateEoriController(mcc, viewUpdateEori, viewConfirmUpdate, viewEoriProblem, testAuthAction, enrolmentService)

  override def beforeEach(): Unit = {
    reset(mockAuthConnector, enrolmentService)
  }

  "showPage /" should {
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

  "continueUpdateEori" should {
    "redirect to show confirmation page when entered information is correct" in withSignedInUser {
      val existingEori = "GB944494423491"
      val newEori = "GB944494423492"
      val fakeRequestWithBody = FakeRequest("POST", "/")
        .withFormUrlEncodedBody(
          "existing-eori" -> existingEori,
          "date-of-establishment.day" -> "04",
          "date-of-establishment.month" -> "11",
          "date-of-establishment.year" -> "1997",
          "new-eori" -> newEori
        )
      when(enrolmentService.getEnrolments(meq(Eori("GB944494423491")), meq(stringToLocalDate("04/11/1997")))(any()))
        .thenReturn(Future.successful(Seq(("HMRC-GVMS-ORG", ValidateEori.TRUE))))
      val result = controller.continueUpdateEori(fakeRequestWithBody)
      status(result) shouldBe OK
    }

    "show page again with error if existing EORI number date is not matching with Eori numbers date" in withSignedInUser {
      val existingEori = "GB944494423491"
      val newEori = "GB944494423492"
      val fakeRequestWithBody = FakeRequest("POST", "/")
        .withFormUrlEncodedBody(
          "existing-eori" -> existingEori,
          "date-of-establishment.day" -> "04",
          "date-of-establishment.month" -> "11",
          "date-of-establishment.year" -> "1997",
          "new-eori" -> newEori
        )
      when(enrolmentService.getEnrolments(meq(Eori("GB944494423491")), meq(stringToLocalDate("04/11/1997")))(any()))
        .thenReturn(Future.successful(Seq(("HMRC-GVMS-ORG", ValidateEori.ESTABLISHMENT_DATE_WRONG))))
      val result = controller.continueUpdateEori(fakeRequestWithBody)
      status(result) shouldBe BAD_REQUEST
      contentAsString(result) should include("establishment date must match establishment date of the current EORI number")
    }

    "show page again with error if existing EORI number is not entered" in withSignedInUser {
      val fakeRequestWithBody = FakeRequest("POST", "/")
        .withFormUrlEncodedBody(
          "existing-eori" -> "",
          "date-of-establishment.day" -> "04",
          "date-of-establishment.month" -> "11",
          "date-of-establishment.year" -> "1997",
          "new-eori" -> "GB944494423492"
        )
      val result = controller.continueUpdateEori(fakeRequestWithBody)
      status(result) shouldBe BAD_REQUEST
      contentAsString(result) should include(s"Enter the trader’s current EORI number")
    }

    "show page again with error if existing EORI number is wrong" in withSignedInUser {
      val fakeRequestWithBody = FakeRequest("POST", "/")
        .withFormUrlEncodedBody(
          "existing-eori" -> "GB944",
          "date-of-establishment.day" -> "04",
          "date-of-establishment.month" -> "11",
          "date-of-establishment.year" -> "1997",
          "new-eori" -> "GB944494423492"
        )
      val result = controller.continueUpdateEori(fakeRequestWithBody)
      status(result) shouldBe BAD_REQUEST
      contentAsString(result) should include(s"The trader’s current EORI number must start with the letters GB , followed by 12 digits")
    }

    "show page again with error if new EORI number is empty" in withSignedInUser {
      val fakeRequestWithBody = FakeRequest("POST", "/")
        .withFormUrlEncodedBody(
          "existing-eori" -> "GB944494423491",
          "date-of-establishment.day" -> "04",
          "date-of-establishment.month" -> "11",
          "date-of-establishment.year" -> "1997",
          "new-eori" -> ""
        )
      val result = controller.continueUpdateEori(fakeRequestWithBody)
      status(result) shouldBe BAD_REQUEST
      contentAsString(result) should include(s"Enter the trader’s new EORI number")
    }

    "show page again with error if new EORI number is wrong" in withSignedInUser {
      val fakeRequestWithBody = FakeRequest("POST", "/")
        .withFormUrlEncodedBody(
          "existing-eori" -> "GB944494423491",
          "date-of-establishment.day" -> "04",
          "date-of-establishment.month" -> "11",
          "date-of-establishment.year" -> "1997",
          "new-eori" -> "GB9444"
        )
      val result = controller.continueUpdateEori(fakeRequestWithBody)
      status(result) shouldBe BAD_REQUEST
      contentAsString(result) should include(s"The trader’s new EORI number must start with the letters GB , followed by 12 digits")
    }

    "show page again with error if day of DOE is not entered" in withSignedInUser {
      val fakeRequestWithBody = FakeRequest("POST", "/")
        .withFormUrlEncodedBody(
          "existing-eori" -> "GB944494423491",
          "date-of-establishment.month" -> "04",
          "date-of-establishment.year" -> "1997",
          "new-eori" -> "GB944494423492"
        )
      val result = controller.continueUpdateEori(fakeRequestWithBody)
      status(result) shouldBe BAD_REQUEST
      contentAsString(result) should include(s"The date the trader was established must include a day")
    }

    "show page again with error if month of DOE is not entered" in withSignedInUser {
      val fakeRequestWithBody = FakeRequest("POST", "/")
        .withFormUrlEncodedBody(
          "existing-eori" -> "GB944494423491",
          "date-of-establishment.day" -> "04",
          "date-of-establishment.year" -> "1997",
          "new-eori" -> "GB944494423492"
        )
      val result = controller.continueUpdateEori(fakeRequestWithBody)
      status(result) shouldBe BAD_REQUEST
      contentAsString(result) should include(s"The date the trader was established must include a month")
    }

    "show page again with error if year of DOE is not entered" in withSignedInUser {
      val fakeRequestWithBody = FakeRequest("POST", "/")
        .withFormUrlEncodedBody(
          "existing-eori" -> "GB944494423491",
          "date-of-establishment.day" -> "04",
          "date-of-establishment.month" -> "11",
          "new-eori" -> "GB944494423492"
        )
      val result = controller.continueUpdateEori(fakeRequestWithBody)
      status(result) shouldBe BAD_REQUEST
      contentAsString(result) should include(s"The date the trader was established must include a year")
    }

    "show page again with error if day and month of DOE is not entered" in withSignedInUser {
      val fakeRequestWithBody = FakeRequest("POST", "/")
        .withFormUrlEncodedBody(
          "existing-eori" -> "GB944494423491",
          "date-of-establishment.year" -> "2000",
          "new-eori" -> "GB944494423492"
        )
      val result = controller.continueUpdateEori(fakeRequestWithBody)
      status(result) shouldBe BAD_REQUEST
      contentAsString(result) should include(s"The date the trader was established must be a real date. Enter a day and a month")
    }

    "show page again with error if DOE is not entered" in withSignedInUser {
      val fakeRequestWithBody = FakeRequest("POST", "/")
        .withFormUrlEncodedBody(
          "existing-eori" -> "GB944494423491",
          "new-eori" -> "GB944494423492"
        )
      val result = controller.continueUpdateEori(fakeRequestWithBody)
      status(result) shouldBe BAD_REQUEST
      contentAsString(result) should include(s"Enter the date that the trader was established")
    }

    "show page again with error if DOE is wrong" in withSignedInUser {
      val fakeRequestWithBody = FakeRequest("POST", "/")
        .withFormUrlEncodedBody(
          "existing-eori" -> "GB944494423491",
          "date-of-establishment.day" -> "04",
          "date-of-establishment.month" -> "AA",
          "date-of-establishment.year" -> "YEAR",
          "new-eori" -> "GB944494423492"
        )
      val result = controller.continueUpdateEori(fakeRequestWithBody)
      status(result) shouldBe BAD_REQUEST
      contentAsString(result) should include(s"The date the trader was established must be a real date")
    }

    "show page again with error if DOE entered as future date" in withSignedInUser {
      val futureDate = LocalDate.now().plusDays(2)
      val fakeRequestWithBody = FakeRequest("POST", "/")
        .withFormUrlEncodedBody(
          "existing-eori" -> "GB944494423491",
          "date-of-establishment.day" -> futureDate.getDayOfMonth.toString,
          "date-of-establishment.month" -> futureDate.getMonthValue.toString,
          "date-of-establishment.year" -> futureDate.getYear.toString,
          "new-eori" -> "GB944494423492"
        )
      val result = controller.continueUpdateEori(fakeRequestWithBody)
      status(result) shouldBe BAD_REQUEST
      contentAsString(result) should include(s"The date the trader was established must be in the past")
    }

    "show page again with error if year of DOE is less than four digit" in withSignedInUser {
      val fakeRequestWithBody = FakeRequest("POST", "/")
        .withFormUrlEncodedBody(
          "existing-eori" -> "GB944494423491",
          "date-of-establishment.day" -> "04",
          "date-of-establishment.month" -> "11",
          "date-of-establishment.year" -> "200",
          "new-eori" -> "GB944494423492"
        )
      val result = controller.continueUpdateEori(fakeRequestWithBody)
      status(result) shouldBe BAD_REQUEST
      contentAsString(result) should include(s"The date the trader was established must be a real date")
    }

    "show page again with multiple errors and with bad request status" in withSignedInUser {
      val fakeRequestWithBody = FakeRequest("POST", "/")
        .withFormUrlEncodedBody()
      val result = controller.continueUpdateEori(fakeRequestWithBody)
      status(result) shouldBe BAD_REQUEST
      contentAsString(result) should include(s"Enter the trader’s current EORI number")
      contentAsString(result) should include(s"Enter the trader’s new EORI number")
      contentAsString(result) should include(s"Enter the date that the trader was established")
    }

    "redirect to STRIDE login for not logged-in user" in withNotSignedInUser {
      val fakeRequestWithBody = FakeRequest("POST", "/")
        .withFormUrlEncodedBody(
          "existing-eori" -> "GB94449442349",
          "date-of-establishment.day" -> "04",
          "date-of-establishment.month" -> "11",
          "date-of-establishment.year" -> "1997",
          "new-eori" -> "GB94449442340"
        )
      val result = controller.continueUpdateEori(fakeRequestWithBody)
      status(result) shouldBe SEE_OTHER
      val Some(redirectURL) = redirectLocation(result)
      redirectURL should include("/stride/sign-in")
    }
  }

  "confirmUpdateEori" should {
    "complete the confirmation if user select confirm" in withSignedInUser {
      val oldEori = "GB94449442349"
      val newEori = "GB94449442340"
      val establishmentDate = "04/11/1997"
      val fakeRequestWithBody = FakeRequest("POST", "/")
        .withFormUrlEncodedBody(
          "existing-eori" -> oldEori,
          "date-of-establishment" -> establishmentDate,
          "new-eori" -> newEori,
          "enrolment-list" -> s"${EnrolmentKey.HMRC_CUS_ORG.serviceName},${EnrolmentKey.HMRC_ATAR_ORG.serviceName}",
          "not-updatable-enrolment-list" -> "",
          "confirm" -> "true"
        )

      when(enrolmentService.update(meq(Eori(oldEori)), meq(stringToLocalDate(establishmentDate)), meq(Eori(newEori)), meq(EnrolmentKey.HMRC_CUS_ORG))(any()))
        .thenReturn(Future.successful(Right(Enrolment(Seq.empty, Seq.empty))))

      when(enrolmentService.update(meq(Eori(oldEori)), meq(stringToLocalDate(establishmentDate)), meq(Eori(newEori)), meq(EnrolmentKey.HMRC_ATAR_ORG))(any()))
        .thenReturn(Future.successful(Right(Enrolment(Seq.empty, Seq.empty))))

      val result = controller.confirmUpdateEori(fakeRequestWithBody)
      val Some(redirectURL) = redirectLocation(result)
      status(result) shouldBe SEE_OTHER
      redirectURL should include(s"/customs-update-eori-admin-frontend/success?cancelOrUpdate=Update-Eori&oldEoriNumber=$oldEori&newEoriNumber=$newEori")
    }

    "redirect to STRIDE login for not logged-in user" in withNotSignedInUser {
      val fakeRequestWithBody = FakeRequest("POST", "/")
        .withFormUrlEncodedBody(
          "existing-eori" -> "GB94449442349",
          "date-of-establishment" -> "04/11/1997",
          "new-eori" -> "GB94449442340",
          "enrolment-list" -> s"${EnrolmentKey.HMRC_CUS_ORG.serviceName}, ${EnrolmentKey.HMRC_ATAR_ORG.serviceName}",
          "confirm" -> "true"
        )

      val result = controller.confirmUpdateEori(fakeRequestWithBody)
      val Some(redirectURL) = redirectLocation(result)
      status(result) shouldBe SEE_OTHER
      redirectURL should include("/stride/sign-in")
    }
  }

}
