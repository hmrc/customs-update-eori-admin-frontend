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

import models.EnrolmentKey.HMRC_CUS_ORG
import models.EoriEventEnum.UPDATE
import models.{Eori, ErrorMessage, GroupId}
import org.mockito.ArgumentMatchers.{eq as meq, *}
import org.mockito.Mockito.*
import play.api.http.Status.*
import uk.gov.hmrc.http.{HttpResponse, StringContextOps}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class DeAllocateGroupConnectorSpec extends ConnectorSpecBase {

  private val connector = new DeAllocateGroupConnector(mockHttpClient, mockAppConfig, mockAuditable)

  "The Delete Enrolment Connector" should {
    "call the de-enrolment service with a DELETE command with the correct url" in {
      when(mockHttpClient.delete(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute(any(), any())).thenReturn(Future.successful(HttpResponse(NO_CONTENT, "")))
      whenReady(
        connector
          .deAllocateGroup(UPDATE, Eori("GB1234567890"), HMRC_CUS_ORG, GroupId("90ccf333-65d2-4bf2-a008-01dfca702161"))
      ) { _ =>
        verify(mockHttpClient).delete(
          meq(
            url"http://localhost:1222/groups/90ccf333-65d2-4bf2-a008-01dfca702161/enrolments/HMRC-CUS-ORG~EORINumber~GB1234567890"
          )
        )(any())
      }
    }

    "return an error message if the delete request fails " in {
      when(
        mockHttpClient.delete(
          meq(
            url"http://localhost:1222/groups/90ccf333-65d2-4bf2-a008-01dfca706334/enrolments/HMRC-CUS-ORG~EORINumber~GB1234566634"
          )
        )(any())
      ).thenReturn(mockRequestBuilder)

      when(mockRequestBuilder.execute(any(), any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "")))
      val result = connector
        .deAllocateGroup(
          UPDATE,
          Eori("GB1234566634"),
          HMRC_CUS_ORG,
          GroupId("90ccf333-65d2-4bf2-a008-01dfca706334")
        )
        .futureValue
      result shouldBe Left(ErrorMessage("Delete enrolment failed with HTTP status: 400"))
    }
  }
}
