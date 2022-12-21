/*
 * Copyright 2022 HM Revenue & Customs
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

import models.{Eori, ErrorMessage, UserId}
import org.mockito.ArgumentMatchers.{any, eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Ignore, Matchers, WordSpec}
import play.api.test.Helpers.BAD_REQUEST
import play.api.{ConfigLoader, Configuration}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

@Ignore
class QueryUsersConnectorSpec
    extends WordSpec
    with Matchers
    with ScalaFutures
    with MockitoSugar
    with BeforeAndAfterEach {

  private implicit val mockHeaderCarrier = mock[HeaderCarrier]
  private val mockHttpClient = mock[HttpClient]
  private val mockConfig = mock[Configuration]

  private val connector = new QueryUsersConnector(mockHttpClient, mockConfig)

  override def beforeEach(): Unit = {
    reset(mockConfig, mockHttpClient)

    when(
      mockConfig.get[String](meq("enrolment.service.context"))(
        any[ConfigLoader[String]])).thenReturn("http://localhost:1234")

    when(
      mockHttpClient.GET(endsWith("GB1234567890/users"), any[Seq[(String, String)]], any[Seq[(String, String)]])(
        any[HttpReads[Either[UpstreamErrorResponse, Users]]],
        any[HeaderCarrier],
        any[ExecutionContext])).thenReturn(
      Future.successful(Right(Users(Seq("ABCEDEFGI1234567"), Seq.empty))))
  }

  "The Query Users Connector" should {
    "call the query users service with a GET command with the correct url" in {
      whenReady(connector.queryUsers(Eori("GB1234567890"))) { _ =>
        verify(mockHttpClient).GET(meq(
          "http://localhost:1234/enrolment-store/enrolments/HMRC-CUS-ORG~EORINumber~GB1234567890/users"), 
          any[Seq[(String, String)]],
          any[Seq[(String, String)]])(
          any[HttpReads[Either[UpstreamErrorResponse, Users]]],
          any[HeaderCarrier],
          any[ExecutionContext])
      }
    }

    "return a User ID for a valid EORI" in {
      val Right(userId) = connector.queryUsers(Eori("GB1234567890")).futureValue
      userId shouldBe UserId("ABCEDEFGI1234567")
    }

    "return an error message for an invalid EORI" in {
      when(
        mockHttpClient.GET(endsWith("GB8888877777/users"), any[Seq[(String, String)]], any[Seq[(String, String)]])(
          any[HttpReads[Either[UpstreamErrorResponse, Users]]],
          any[HeaderCarrier],
          any[ExecutionContext])).thenReturn(Future.successful(Left(UpstreamErrorResponse("failed", BAD_REQUEST))))
      val Left(ErrorMessage(error)) =
        connector.queryUsers(Eori("GB8888877777")).futureValue
      error shouldBe "Could not find User for existing EORI: GB8888877777"
    }
  }
}
