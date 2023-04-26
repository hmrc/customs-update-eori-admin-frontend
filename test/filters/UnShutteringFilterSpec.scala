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

package filters

import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import views.html.ShutterView

class UnShutteringFilterSpec
    extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with OptionValues with Injecting {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure("shuttered" -> false)
    .build()

  val view = inject[ShutterView]

  "UnShuttering filter" should {
    "unShutter when the `shuttered` config property is false" in {

      val result = route(app, FakeRequest(GET, controllers.routes.EoriActionController.showPage.url)).value

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(
        "/stride/sign-in?successURL=%2Fmanage-eori-number&origin=customs-update-eori-admin-frontend"
      )
    }
  }
}
