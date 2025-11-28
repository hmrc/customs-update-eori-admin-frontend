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

class ReAllocateGroupConnectorSpec extends ConnectorSpecBase {

  private val connector = new ReAllocateGroupConnector(mockHttpClient, mockAppConfig, mockAuditable)

  "The ReEnrolment Connector" should {
    "call the re-enrolment service with a POST command with the correct url" in {
      when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute(any(), any())).thenReturn(Future.successful(HttpResponse(CREATED, "")))
      whenReady(connector.reAllocate(Eori("GB1234567890"), HMRC_CUS_ORG, UserId("AB123"), GroupId("90ccf333-65d2"))) {
        _ =>
          verify(mockHttpClient).post(
            meq(url"http://localhost:1222/groups/90ccf333-65d2/enrolments/HMRC-CUS-ORG~EORINumber~GB1234567890")
          )(any())
      }
    }

    "return an error message if the post request fails " in {
      when(
        mockHttpClient.post(
          meq(url"http://localhost:1222/groups/90ccf333-65d2/enrolments/HMRC-CUS-ORG~EORINumber~GB1234566634")
        )(any())
      ).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute(any(), any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "")))
      val result = connector
        .reAllocate(
          Eori("GB1234566634"),
          HMRC_CUS_ORG,
          UserId("AB234"),
          GroupId("90ccf333-65d2")
        )
        .futureValue
      result.isLeft should be(true)
      result.left.getOrElse(ErrorMessage("")).message should startWith("Allocate group failed with HTTP status: 400")
    }
  }
}
