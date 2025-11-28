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

import models.*
import models.EnrolmentKey.HMRC_CUS_ORG
import org.mockito.ArgumentMatchers.{eq as meq, *}
import org.mockito.Mockito.*
import play.api.http.Status.*
import uk.gov.hmrc.http.{HttpResponse, StringContextOps}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class UpsertKnownFactsConnectorSpec extends ConnectorSpecBase {

  private val connector = new UpsertKnownFactsConnector(mockHttpClient, mockAppConfig, mockAuditable)

  "The Upsert Enrolment Connector" should {
    "return a success status for a valid upsert" in {
      val enrolment = Enrolment(Seq.empty, Seq(KeyValue("DateOfEstablishment", "02/06/2003")))

      when(
        mockHttpClient.put(
          meq(url"http://localhost:1222/enrolments/HMRC-CUS-ORG~EORINumber~GB12349876")
        )(any())
      ).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute(any(), any())).thenReturn(Future.successful(HttpResponse(NO_CONTENT, "")))

      val statusCode = connector.upsert(Eori("GB12349876"), HMRC_CUS_ORG, enrolment).futureValue
      statusCode shouldBe Right(NO_CONTENT)
    }

    "return an error message for an invalid upsert" in {
      val emptyEnrolment = Enrolment(Seq.empty, Seq.empty)
      when(mockHttpClient.put(any)(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute(any(), any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "")))

      val result = connector
        .upsert(Eori("GB9999999999"), HMRC_CUS_ORG, emptyEnrolment)
        .futureValue
      result shouldBe Left(ErrorMessage("Upsert failed with HTTP status: 400"))
    }
  }
}
