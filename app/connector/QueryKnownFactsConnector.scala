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

import models._
import play.api.Configuration
import play.api.libs.json.{Json, OWrites, Reads}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class QueryKnownFactsResponse(service: String, enrolments: Seq[Enrolment])

object QueryKnownFactsResponse {
  implicit val kvReads: Reads[KeyValue] = Json.reads[KeyValue]
  implicit val eReads: Reads[Enrolment] = Json.reads[Enrolment]
  implicit val kfrReads: Reads[QueryKnownFactsResponse] =
    Json.reads[QueryKnownFactsResponse]
}

case class QueryKnownFactsRequest(service: String, knownFacts: Seq[KeyValue])

object QueryKnownFactsRequest {
  implicit val kvWrites: OWrites[KeyValue] = Json.writes[KeyValue]
  implicit val kfrWrites: OWrites[QueryKnownFactsRequest] =
    Json.writes[QueryKnownFactsRequest]
}

class QueryKnownFactsConnector @Inject()(
    httpClient: HttpClient,
    config: Configuration)(implicit ec: ExecutionContext)
    extends {
  val configuration = config
} with ContextBuilder {

  def queryKnownFacts(eori: Eori, dateOfEstablishment: LocalDate)(
      implicit hc: HeaderCarrier): Future[Either[ErrorMessage, Enrolment]] = {
    val url = s"$enrolmentServiceBaseContext/enrolment-store/enrolments"
    val key = "DateOfEstablishment"
    val req = QueryKnownFactsRequest("HMRC-CUS-ORG",
                                     Seq(KeyValue("EORINumber", eori.eori)))

    httpClient
      .POST[QueryKnownFactsRequest, Either[UpstreamErrorResponse, QueryKnownFactsResponse]](url, req)
      .map {
        case Left(UpstreamErrorResponse(_, _, _, _)) =>
          Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $eori"))
        case Right(queryKnownFactsResponse) =>
          verifyDateOfEstablishment(dateOfEstablishment, key, queryKnownFactsResponse) match {
            case Some(true) => Right(queryKnownFactsResponse.enrolments.head)
            case Some(false) => Left(ErrorMessage("The date you have entered does not match our records, please try again"))
            case _ => Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $eori"))
          }
      }
  }

  private def verifyDateOfEstablishment(
      dateOfEstablishment: LocalDate,
      key: String,
      queryKnownFactsResponse: QueryKnownFactsResponse): Option[Boolean] = {
    queryKnownFactsResponse.enrolments.head.verifiers
      .find(_.key == key)
      .map(d => stringToLocalDate(d.value) == dateOfEstablishment)
  }

  private def stringToLocalDate(date: String) = {
    val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    LocalDate.parse(date, dateTimeFormatter)
  }
}
