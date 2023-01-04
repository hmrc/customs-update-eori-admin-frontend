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
import play.api.http.Status.{NO_CONTENT, OK}
import play.api.libs.json.Writes
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}


class QueryKnownFactsConnectorSpec extends ConnectorSpecBase {

  private val connector = new QueryKnownFactsConnector(mockHttpClient, mockAppConfig)

  "The Query Known Facts Connector" should {
    "return an Enrolment for a valid EORI" in {
      val response =
        """
          |{
          |  "service": "HMRC-CUS-ORG",
          |  "enrolments": [
          |     {
          |       "identifiers": [
          |         {
          |            "key": "EORINumber",
          |            "value": "GB1234567890"
          |         }
          |       ],
          |       "verifiers": [
          |          {
          |             "key": "NINO",
          |             "value": "NZ123456A"
          |          },
          |          {
          |             "key": "Postcode",
          |             "value": "BD99 3LZ"
          |          },
          |          {
          |             "key": "DateOfEstablishment",
          |             "value": "01/01/2011"
          |          }
          |       ]
          |     }
          |  ]
          |}""".stripMargin
      val request = QueryKnownFactsRequest("HMRC-CUS-ORG", Seq(KeyValue("EORINumber", "GB1234567890")))

      when(
        mockHttpClient.POST(anyString,
          meq(request),
          any[Seq[(String, String)]])(
          any[Writes[QueryKnownFactsRequest]],
          any[HttpReads[HttpResponse]],
          any[HeaderCarrier],
          any[ExecutionContext]))
        .thenReturn(Future.successful(HttpResponse(OK, response)))

      val Right(enrolment) = connector
        .query(Eori("GB1234567890"), HMRC_CUS_ORG, LocalDate.of(2011, 1, 1))
        .futureValue

      enrolment.identifiers shouldBe Seq(KeyValue("EORINumber", "GB1234567890"))
      enrolment.verifiers shouldBe Seq(KeyValue("NINO", "NZ123456A"),
        KeyValue("Postcode", "BD99 3LZ"),
        KeyValue("DateOfEstablishment", "01/01/2011"))
    }

    "return an error message for an invalid EORI" in {
      val request = QueryKnownFactsRequest("HMRC-CUS-ORG", Seq(KeyValue("EORINumber", "GB9999999999")))

      when(
        mockHttpClient.POST(anyString,
          meq(request),
          any[Seq[(String, String)]])(
          any[Writes[QueryKnownFactsRequest]],
          any[HttpReads[HttpResponse]],
          any[HeaderCarrier],
          any[ExecutionContext]))
        .thenReturn(Future.successful(HttpResponse(NO_CONTENT, "")))

      val Left(ErrorMessage(error)) = connector
        .query(Eori("GB9999999999"), HMRC_CUS_ORG, LocalDate.now())
        .futureValue
      error shouldBe "Could not find Known Facts for existing EORI: GB9999999999"
    }
  }
}
