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
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.ShutterView

class MaintenanceControllerSpec
    extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with AuthenticationBehaviours with MockitoSugar
    with BeforeAndAfterEach {
  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "metrics.jvm"     -> false,
        "metrics.enabled" -> false
      )
      .build()

  val fakeRequest = FakeRequest("GET", "/maintenance")

  private val mcc = app.injector.instanceOf[MessagesControllerComponents]
  private val view = app.injector.instanceOf[ShutterView]

  private val controller = MaintenanceController(mcc, view)

  override def beforeEach(): Unit =
    reset(mockAuthConnector)

  "Get /maintenance" should {
    "return 503" in {
      val result = controller.get(fakeRequest)
      status(result) shouldBe Status.SERVICE_UNAVAILABLE
      contentType(result) shouldBe Some("text/html")
      contentAsString(result) should include(
        s"Eori toolkit service is under maintenance. The service will be available soon."
      )
    }
  }
}
