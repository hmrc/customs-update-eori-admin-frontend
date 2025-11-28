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
import models.{Eori, ErrorMessage, GroupId}
import org.mockito.ArgumentMatchers.{eq as meq, *}
import org.mockito.Mockito.*
import play.api.test.Helpers.BAD_REQUEST
import uk.gov.hmrc.http.{StringContextOps, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class QueryGroupsConnectorSpec extends ConnectorSpecBase {

  private val connector = new QueryGroupsConnector(mockHttpClient, mockAppConfig)

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(
      mockHttpClient.get(
        meq(url"http://localhost:1234/enrolment-store/enrolments/HMRC-CUS-ORG~EORINumber~GB1234567890/groups")
      )(any())
    ).thenReturn(mockRequestBuilder)

    when(mockRequestBuilder.execute(any(), any()))
      .thenReturn(Future.successful(Right(Groups(Seq("90ccf333-65d2-4bf2-a008-01dfca702161"), Seq.empty))))
  }

  "The Query Groups Connector" should {
    "call the query groups service with a GET command with the correct url" in
      whenReady(connector.query(Eori("GB1234567890"), HMRC_CUS_ORG)) { _ =>
        verify(mockHttpClient).get(
          meq(url"http://localhost:1234/enrolment-store/enrolments/HMRC-CUS-ORG~EORINumber~GB1234567890/groups")
        )(any())
      }

    "return a Group ID for a valid EORI" in {
      val groupId = connector.query(Eori("GB1234567890"), HMRC_CUS_ORG).futureValue
      groupId shouldBe Right(GroupId("90ccf333-65d2-4bf2-a008-01dfca702161"))
    }

    "return an error message for an invalid EORI" in {
      when(
        mockHttpClient.get(
          meq(url"http://localhost:1234/enrolment-store/enrolments/HMRC-CUS-ORG~EORINumber~GB8888877777/groups")
        )(any())
      ).thenReturn(mockRequestBuilder)

      when(mockRequestBuilder.execute(any(), any()))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("failed", BAD_REQUEST))))
      val result = connector.query(Eori("GB8888877777"), HMRC_CUS_ORG).futureValue
      result shouldBe Left(ErrorMessage("Could not find Group for existing EORI: GB8888877777"))
    }
  }
}
