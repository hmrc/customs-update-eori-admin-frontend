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

import models.EnrolmentKey.HMRC_CUS_ORG
import models.{Eori, ErrorMessage, UserId}
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import play.api.test.Helpers.BAD_REQUEST
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class QueryUsersConnectorSpec extends ConnectorSpecBase {

  private val connector = new QueryUsersConnector(mockHttpClient, mockAppConfig)

  override def beforeEach(): Unit = {
    super.beforeEach()

    when(
      mockHttpClient.GET(endsWith("GB1234567890/users"), any[Seq[(String, String)]], any[Seq[(String, String)]])(
        any[HttpReads[Either[UpstreamErrorResponse, Users]]],
        any[HeaderCarrier],
        any[ExecutionContext]
      )
    ).thenReturn(Future.successful(Right(Users(Seq("ABCEDEFGI1234567"), Seq.empty))))
  }

  "The Query Users Connector" should {
    "call the query users service with a GET command with the correct url" in {
      whenReady(connector.query(Eori("GB1234567890"), HMRC_CUS_ORG)) { _ =>
        verify(mockHttpClient).GET(
          meq("http://localhost:1234/enrolment-store/enrolments/HMRC-CUS-ORG~EORINumber~GB1234567890/users"),
          any[Seq[(String, String)]],
          any[Seq[(String, String)]]
        )(any[HttpReads[Either[UpstreamErrorResponse, Users]]], any[HeaderCarrier], any[ExecutionContext])
      }
    }

    "return a User ID for a valid EORI" in {
      val userId = connector.query(Eori("GB1234567890"), HMRC_CUS_ORG).futureValue
      userId shouldBe Right(UserId("ABCEDEFGI1234567"))
    }

    "return an error message for an invalid EORI" in {
      when(
        mockHttpClient.GET(endsWith("GB8888877777/users"), any[Seq[(String, String)]], any[Seq[(String, String)]])(
          any[HttpReads[Either[UpstreamErrorResponse, Users]]],
          any[HeaderCarrier],
          any[ExecutionContext]
        )
      ).thenReturn(Future.successful(Left(UpstreamErrorResponse("failed", BAD_REQUEST))))
      val result = connector.query(Eori("GB8888877777"), HMRC_CUS_ORG).futureValue
      result shouldBe Left(ErrorMessage("Could not find User for existing EORI: GB8888877777"))
    }
  }
}
