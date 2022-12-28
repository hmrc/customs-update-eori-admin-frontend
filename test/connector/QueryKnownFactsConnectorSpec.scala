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

import models.EnrolmentKey.HMRC_CUS_ORG
import models._
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import play.api.libs.json.Writes
import play.api.test.Helpers.BAD_REQUEST
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, UpstreamErrorResponse}

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}


class QueryKnownFactsConnectorSpec extends ConnectorSpecBase {

  private val connector = new QueryKnownFactsConnector(mockHttpClient, mockConfig)

  "The Query Known Facts Connector" should {
    "return an Enrolment for a valid EORI" in {
      val response = QueryKnownFactsResponse(
        "HMRC-CUS-ORG",
        Seq(
          Enrolment(
            Seq(KeyValue("EORINumber", "GB1234567890")),
            Seq(KeyValue("NINO", "NZ123456A"),
              KeyValue("Postcode", "BD99 3LZ"),
              KeyValue("DateOfEstablishment", "01/01/2011"))
          ))
      )
      val request =
        QueryKnownFactsRequest("HMRC-CUS-ORG",
          Seq(KeyValue("EORINumber", "GB1234567890")))
      when(
        mockHttpClient.POST(anyString,
          meq(request),
          any[Seq[(String, String)]])(
          any[Writes[QueryKnownFactsRequest]],
          any[HttpReads[Either[UpstreamErrorResponse, QueryKnownFactsResponse]]],
          any[HeaderCarrier],
          any[ExecutionContext])).thenReturn(Future.successful(Right(response)))

      val Right(enrolment) = connector
        .query(Eori("GB1234567890"), HMRC_CUS_ORG, LocalDate.of(2011, 1, 1))
        .futureValue
      enrolment.identifiers shouldBe Seq(KeyValue("EORINumber", "GB1234567890"))
      enrolment.verifiers shouldBe Seq(KeyValue("NINO", "NZ123456A"),
        KeyValue("Postcode", "BD99 3LZ"),
        KeyValue("DateOfEstablishment",
          "01/01/2011"))
    }

    "return an error message for an invalid EORI" in {
      val request =
        QueryKnownFactsRequest("HMRC-CUS-ORG",
          Seq(KeyValue("EORINumber", "GB9999999999")))
      when(
        mockHttpClient.POST(anyString,
          meq(request),
          any[Seq[(String, String)]])(
          any[Writes[QueryKnownFactsRequest]],
          any[HttpReads[Either[UpstreamErrorResponse, QueryKnownFactsResponse]]],
          any[HeaderCarrier],
          any[ExecutionContext])).thenReturn(Future.successful(Left(UpstreamErrorResponse("failed", BAD_REQUEST))))

      val Left(ErrorMessage(error)) = connector
        .query(Eori("GB9999999999"), HMRC_CUS_ORG, LocalDate.now())
        .futureValue
      error shouldBe "Could not find Known Facts for existing EORI: GB9999999999"
    }
  }
}
