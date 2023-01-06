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

class UpsertKnownFactsConnectorSpec extends ConnectorSpecBase {

  private val connector = new UpsertKnownFactsConnector(mockHttpClient, mockAppConfig)

  "The Upsert Enrolment Connector" should {
    "return a success status for a valid upsert" in {
      val enrolment = Enrolment(Seq.empty, Seq(KeyValue("DateOfEstablishment", "02/06/2003")))

      when(
        mockHttpClient.PUT(
          meq("http://localhost:1222/enrolments/HMRC-CUS-ORG~EORINumber~GB12349876"),
          meq(UpsertKnownFactsRequest(enrolment.verifiers)), any[Seq[(String, String)]])(
          any[Writes[UpsertKnownFactsRequest]],
          any[HttpReads[HttpResponse]],
          any[HeaderCarrier],
          any[ExecutionContext]))
        .thenReturn(Future.successful(HttpResponse(NO_CONTENT, "")))

      val Right(statusCode) = connector.upsert(Eori("GB12349876"), HMRC_CUS_ORG, enrolment)
        .futureValue
      statusCode shouldBe NO_CONTENT
    }

    "return an error message for an invalid upsert" in {
      val emptyEnrolment = Enrolment(Seq.empty, Seq.empty)
      when(
        mockHttpClient.PUT(anyString, meq(UpsertKnownFactsRequest(Seq.empty)), any[Seq[(String, String)]])
        (any[Writes[UpsertKnownFactsRequest]], any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "")))

      val Left(ErrorMessage(error)) = connector
        .upsert(Eori("GB9999999999"), HMRC_CUS_ORG, emptyEnrolment)
        .futureValue
      error shouldBe "Upsert failed with HTTP status: 400"
    }
  }
}
