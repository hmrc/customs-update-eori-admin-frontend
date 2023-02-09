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

import config.AppConfig
import models.LocalDateBinder.stringToLocalDate
import models.EnrolmentKey.EnrolmentKeyType
import models._
import play.api.Logging
import play.api.http.Status.{NO_CONTENT, OK}
import play.api.libs.json.{Json, OWrites, Reads}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import java.time.LocalDate
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

class QueryKnownFactsConnector @Inject()(httpClient: HttpClient, config: AppConfig)(implicit ec: ExecutionContext) extends Logging{

  /**
   * ES20 API call - to validate if EORI and key verifiers match. Data validate API call
   * @param eori
   * @param enrolmentKey
   * @param dateOfEstablishment
   * @param hc
   * @return
   */
  def query(eori: Eori, enrolmentKey: EnrolmentKeyType, dateOfEstablishment: LocalDate)
           (implicit hc: HeaderCarrier): Future[Either[ErrorMessage, Enrolment]] = {
    val url = s"${config.enrolmentStoreProxyServiceUrl}/enrolment-store/enrolments"
    val key = "DateOfEstablishment"
    val req = QueryKnownFactsRequest(enrolmentKey.serviceName, Seq(KeyValue("EORINumber", eori.toString)))

    httpClient.POST[QueryKnownFactsRequest, HttpResponse](url, req)
      .map {
        case response => response.status match {
          case OK =>
            val queryKnownFactsResponse = Json.parse(response.body).as[QueryKnownFactsResponse]
            verifyDateOfEstablishment(dateOfEstablishment, key, queryKnownFactsResponse) match {
              case Some(true) => Right(queryKnownFactsResponse.enrolments.head)
              case _ =>
                logger.error(s"Date not matched for EORI: $eori. Service: ${enrolmentKey.serviceName}")
                Left(ErrorMessage("The date you have entered does not match our records, please try again"))
            }
          case NO_CONTENT => Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $eori"))
          case failStatus =>
            logger.error(s"Query known facts failed with HTTP status:$failStatus for EORI: $eori. Service: ${enrolmentKey.serviceName}")
            Left(ErrorMessage(s"Query known facts failed with HTTP status: $failStatus"))
        }
      }
  }

  private def verifyDateOfEstablishment(dateOfEstablishment: LocalDate,
                                        key: String,
                                        queryKnownFactsResponse: QueryKnownFactsResponse): Option[Boolean] = {
    queryKnownFactsResponse.enrolments.head.verifiers
      .find(_.key == key)
      .map(d => stringToLocalDate(d.value) == dateOfEstablishment)
  }
}
