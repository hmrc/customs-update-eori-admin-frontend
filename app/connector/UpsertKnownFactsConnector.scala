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
import models.events.UpsertKnownFactsEvent
import models.{Enrolment, Eori, ErrorMessage, KeyValue}
import play.api.Logging
import play.api.http.Status.NO_CONTENT
import play.api.libs.json.{Json, OWrites}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class UpsertKnownFactsRequest(verifiers: Seq[KeyValue])

object UpsertKnownFactsRequest {
  implicit val kvWrites: OWrites[KeyValue] = Json.writes[KeyValue]
  implicit val kfrWrites: OWrites[UpsertKnownFactsRequest] = Json.writes[UpsertKnownFactsRequest]

  def apply(e: Enrolment): UpsertKnownFactsRequest =
    UpsertKnownFactsRequest(e.verifiers)
}

class UpsertKnownFactsConnector @Inject() (httpClient: HttpClientV2, config: AppConfig, audit: Auditable)(implicit
  ec: ExecutionContext
) extends Logging {

  /** ES6 API call - update enrolment key (old eori with new eori number)
    *
    * @param eori
    * @param enrolmentKey
    * @param enrolment
    * @param hc
    * @return
    */
  def upsert(eori: Eori, enrolmentKey: EnrolmentKeyType, enrolment: Enrolment)(implicit
    hc: HeaderCarrier
  ): Future[Either[ErrorMessage, Int]] = {
    val strEnrolmentKey = enrolmentKey.getEnrolmentKey(eori)
    val url = s"${config.taxEnrolmentsServiceUrl}/enrolments/$strEnrolmentKey"
    httpClient
      .put(url"$url")
      .withBody(Json.toJson(UpsertKnownFactsRequest(enrolment)))
      .execute[HttpResponse]
      .map { resp =>
        resp.status match {
          case NO_CONTENT =>
            auditCall(url, strEnrolmentKey, enrolment.verifiers)
            Right(NO_CONTENT)
          case failStatus =>
            logger.error(
              s"Upsert failed with HTTP status: $failStatus for existing EORI: $eori. Response: ${resp.body}"
            )
            Left(ErrorMessage(s"Upsert failed with HTTP status: $failStatus"))
        }
      }
  }

  private def auditCall(url: String, enrolmentKey: String, verifiers: Seq[KeyValue])(implicit hc: HeaderCarrier): Unit =
    audit.sendExtendedDataEvent(
      transactionName = "Tax-Enrolments-Call",
      path = url,
      details = Json.toJson(UpsertKnownFactsEvent(enrolmentKey, verifiers)),
      eventType = s"TaxEnrolmentsInsertKnownFactsCall"
    )
}
