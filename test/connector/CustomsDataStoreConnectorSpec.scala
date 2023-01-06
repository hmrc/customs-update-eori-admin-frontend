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

package connector

import audit.Auditor
import models.{Eori, ErrorMessage}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.http.Status._
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class CustomsDataStoreConnectorSpec extends ConnectorSpecBase {

  private val mockAuditor = mock[Auditor]
  private val connector = new CustomsDataStoreConnector(mockHttpClient, mockAppConfig, mockAuditor)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuditor)
  }

  "Customs Data Store Connector" should {
    "return a success status 204 on notification" in {
      when(
        mockHttpClient.POST(anyString,
          any[Eori],
          any[Seq[(String, String)]])(
          any[Writes[Eori]],
          any[HttpReads[HttpResponse]],
          any[HeaderCarrier],
          any[ExecutionContext]))
        .thenReturn(Future.successful(HttpResponse(NO_CONTENT, "")))
      doNothing()
        .when(mockAuditor)
        .sendDataEvent(any(), any(), any(), any())(any[HeaderCarrier])

      val Right(statusCode) = connector.notify(Eori("GB12349876")).futureValue
      statusCode shouldBe NO_CONTENT
    }

    "return an error message for failed notification" in {
      when(
        mockHttpClient.POST(anyString,
          any[Eori],
          any[Seq[(String, String)]])(
          any[Writes[Eori]],
          any[HttpReads[HttpResponse]],
          any[HeaderCarrier],
          any[ExecutionContext]))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, "")))
      doNothing()
        .when(mockAuditor)
        .sendDataEvent(any(), any(), any(), any())(any[HeaderCarrier])

      val Left(ErrorMessage(error)) =
        connector.notify(Eori("GB9999999999")).futureValue
      error shouldBe "notification failed with HTTP status: 500"
    }

    "Eori model object serializes correctly" in {
      Json.toJson(Eori("GB123456780")).toString() shouldBe """{"eori":"GB123456780"}"""
    }

  }
}
