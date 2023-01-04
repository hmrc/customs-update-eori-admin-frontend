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
import models._
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import play.api.http.Status._
import play.api.libs.json.Writes
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class ReAllocateGroupConnectorSpec extends ConnectorSpecBase {

  private val connector = new ReAllocateGroupConnector(mockHttpClient, mockAppConfig)

  "The ReEnrolment Connector" should {
    "call the re-enrolment service with a POST command with the correct url" in {
      when(
        mockHttpClient.POST(any[String],
          any[ReEnrolRequest],
          any[Seq[(String, String)]])(
          any[Writes[ReEnrolRequest]],
          any[HttpReads[HttpResponse]],
          any[HeaderCarrier],
          any[ExecutionContext]))
        .thenReturn(Future.successful(HttpResponse(CREATED, "")))
      whenReady(connector.reAllocate(Eori("GB1234567890"), HMRC_CUS_ORG, UserId("AB123"), GroupId("90ccf333-65d2"))) { _ =>
        verify(mockHttpClient).POST(
          meq("http://localhost:1222/groups/90ccf333-65d2/enrolments/HMRC-CUS-ORG~EORINumber~GB1234567890"),
          meq(ReEnrolRequest("AB123")),
          any[Seq[(String, String)]])(any[Writes[ReEnrolRequest]],
          any[HttpReads[HttpResponse]],
          any[HeaderCarrier],
          any[ExecutionContext])
      }
    }

    "return an error message if the post request fails " in {
      when(
        mockHttpClient.POST(
          meq("http://localhost:1222/groups/90ccf333-65d2/enrolments/HMRC-CUS-ORG~EORINumber~GB1234566634"),
          meq(ReEnrolRequest("AB234")),
          any[Seq[(String, String)]])(
          any[Writes[ReEnrolRequest]],
          any[HttpReads[HttpResponse]],
          any[HeaderCarrier],
          any[ExecutionContext]))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "")))
      val Left(ErrorMessage(message)) = connector.reAllocate(
        Eori("GB1234566634"),
        HMRC_CUS_ORG,
        UserId("AB234"),
        GroupId("90ccf333-65d2")
      ).futureValue
      message should startWith("Allocate group failed with HTTP status: 400")
    }
  }
}
