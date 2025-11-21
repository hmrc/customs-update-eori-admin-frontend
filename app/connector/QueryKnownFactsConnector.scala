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

import audit.Auditable
import config.AppConfig
import models.EnrolmentKey.EnrolmentKeyType
import models.LocalDateBinder.stringToLocalDate
import models.*
import models.events.EnrolmentStoreProxyEvent
import play.api.Logging
import play.api.http.Status.{NO_CONTENT, OK}
import play.api.libs.json.{Json, OWrites, Reads}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

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

class QueryKnownFactsConnector @Inject() (httpClient: HttpClientV2, config: AppConfig, audit: Auditable)(implicit
  ec: ExecutionContext
) extends Logging {

  /** ES20 API call - to validate if EORI and key verifiers match. Data validate API call
    *
    * @param eori
    * @param enrolmentKey
    * @param dateOfEstablishment
    * @param hc
    * @return
    */
  def query(eoriAction: String, eori: Eori, enrolmentKey: EnrolmentKeyType, dateOfEstablishment: LocalDate)(implicit
    hc: HeaderCarrier
  ): Future[Either[ErrorMessage, Enrolment]] = {
    val url = s"${config.enrolmentStoreProxyServiceUrl}/enrolment-store/enrolments"
    val key = "DateOfEstablishment"
    val strEoriNumber = eori.toString
    val serviceName = enrolmentKey.serviceName
    val req = QueryKnownFactsRequest(serviceName, Seq(KeyValue("EORINumber", strEoriNumber)))

    httpClient
      .post(url"$url")
      .withBody(Json.toJson(req))
      .execute[HttpResponse]
      .map { case response =>
        response.status match {
          case OK =>
            val queryKnownFactsResponse = Json.parse(response.body).as[QueryKnownFactsResponse]
            auditCall(url, eoriAction, strEoriNumber, serviceName, queryKnownFactsResponse)
            verifyDateOfEstablishment(dateOfEstablishment, key, queryKnownFactsResponse) match {
              case Some(true) => Right(queryKnownFactsResponse.enrolments.head)
              case _          =>
                logger.error(s"Date not matched for EORI: $eori. Service: $serviceName")
                Left(ErrorMessage("The date you have entered does not match our records, please try again"))
            }
          case NO_CONTENT => Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $eori"))
          case failStatus =>
            logger.error(
              s"Query known facts failed with HTTP status:$failStatus for EORI: $eori. Service: $serviceName"
            )
            Left(ErrorMessage(s"Query known facts failed with HTTP status: $failStatus"))
        }
      }
  }

  private def verifyDateOfEstablishment(
    dateOfEstablishment: LocalDate,
    key: String,
    queryKnownFactsResponse: QueryKnownFactsResponse
  ): Option[Boolean] =
    queryKnownFactsResponse.enrolments.head.verifiers
      .find(_.key == key)
      .map(d => stringToLocalDate(d.value) == dateOfEstablishment)

  private def auditCall(
    url: String,
    eoriAction: String,
    eoriNumber: String,
    serviceName: String,
    response: QueryKnownFactsResponse
  )(implicit hc: HeaderCarrier): Unit =
    audit.sendExtendedDataEvent(
      transactionName = "Enrolment-Store-Proxy-Call",
      path = url,
      details = Json.toJson(EnrolmentStoreProxyEvent(eoriNumber, serviceName, response.enrolments.toList)),
      eventType = s"EnrolmentStoreProxyCallFor$eoriAction"
    )
}
