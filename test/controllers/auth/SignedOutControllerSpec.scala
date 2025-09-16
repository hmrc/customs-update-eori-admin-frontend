/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.auth

import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.OK
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers.{GET, defaultAwaitTimeout, route, status, writeableOf_AnyContentAsEmpty}


class SignedOutControllerSpec
    extends AnyFreeSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar with OptionValues  {
  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "metrics.jvm"     -> false,
        "metrics.enabled" -> false
      )
      .build()

  def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")
  def messagesApi: MessagesApi                         = app.injector.instanceOf[MessagesApi]
  implicit def messages: Messages                      = messagesApi.preferred(fakeRequest)

  "SignedOut Controller" - {

    "must return OK and the correct view for a GET" in {

      val request = FakeRequest(GET, controllers.auth.routes.SignedOutController.onPageLoad.url)
      val result = route(app, request).value

      status(result) mustEqual OK
      Helpers.contentType(result) shouldBe Some("text/html")
      Helpers.charset(result) shouldBe Some("utf-8")
    }
  }

}
