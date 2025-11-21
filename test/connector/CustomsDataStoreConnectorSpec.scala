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

package connector

import models.{Eori, ErrorMessage}
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import play.api.http.Status.*
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class CustomsDataStoreConnectorSpec extends ConnectorSpecBase {

  private val connector = new CustomsDataStoreConnector(mockHttpClient, mockAppConfig, mockAuditable)

  override def beforeEach(): Unit =
    super.beforeEach()

  "Customs Data Store Connector" should {
    "return a success status 204 on notification" in {
      when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute(any(), any())).thenReturn(Future.successful(HttpResponse(NO_CONTENT, "")))

      val statusCode = connector.notify(Eori("GB12349876")).futureValue
      statusCode.isRight shouldBe true
      statusCode shouldBe Right(NO_CONTENT)
    }

    "return an error message for failed notification" in {
      when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute(any(), any()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, "")))

      val error = connector.notify(Eori("GB9999999999")).futureValue
      error.isLeft shouldBe true
      error shouldBe Left(ErrorMessage("Notification failed with HTTP status: 500"))
    }

    "Eori model object serializes correctly" in {
      Json.toJson(Eori("GB123456780")).toString() shouldBe """{"eori":"GB123456780"}"""
    }

  }
}
