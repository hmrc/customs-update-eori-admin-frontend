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
import models.EnrolmentKey.EnrolmentKeyType
import models.{Enrolment, Eori, ErrorMessage, KeyValue}
import play.api.http.Status.NO_CONTENT
import play.api.libs.json.{Json, OWrites}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class UpsertKnownFactsRequest(verifiers: Seq[KeyValue])

object UpsertKnownFactsRequest {
  implicit val kvWrites: OWrites[KeyValue] = Json.writes[KeyValue]
  implicit val kfrWrites: OWrites[UpsertKnownFactsRequest] = Json.writes[UpsertKnownFactsRequest]

  def apply(e: Enrolment): UpsertKnownFactsRequest =
    UpsertKnownFactsRequest(e.verifiers)
}

class UpsertKnownFactsConnector @Inject()(httpClient: HttpClient, config: AppConfig)(implicit ec: ExecutionContext) {

  private def upsert(url: String, enrolment: Enrolment, service: String)
                    (implicit hc: HeaderCarrier): Future[Either[ErrorMessage, Int]] = {
    httpClient.PUT[UpsertKnownFactsRequest, HttpResponse](
      url,
      UpsertKnownFactsRequest(enrolment)) map {
      _.status match {
        case NO_CONTENT => Right(NO_CONTENT)
        case failStatus =>
          Left(ErrorMessage(s"[$service] Upsert failed with HTTP status: $failStatus"))
      }
    }
  }

  def upsertWithESP(eori: Eori, enrolmentKey: EnrolmentKeyType, enrolment: Enrolment)
                   (implicit hc: HeaderCarrier): Future[Either[ErrorMessage, Int]] = {
    val url = s"${config.enrolmentStoreProxyServiceUrl}/enrolment-store/enrolments/${enrolmentKey.getEnrolmentKey(eori)}"
    upsert(url, enrolment, "Enrolment-Store-Proxy")
  }

  def upsertWithTE(eori: Eori, enrolmentKey: EnrolmentKeyType, enrolment: Enrolment)
                  (implicit hc: HeaderCarrier): Future[Either[ErrorMessage, Int]] = {
    val url = s"${config.taxEnrolmentsServiceUrl}/enrolments/${enrolmentKey.getEnrolmentKey(eori)}"
    upsert(url, enrolment, "Tax-Enrolments")
  }
}
