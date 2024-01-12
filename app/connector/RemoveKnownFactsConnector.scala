/*
 * Copyright 2024 HM Revenue & Customs
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
import models.events.RemoveKnownFactsEvent
import models.{Eori, ErrorMessage}
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveKnownFactsConnector @Inject() (httpClient: HttpClient, config: AppConfig, audit: Auditable)(implicit
  ec: ExecutionContext
) extends Logging {

  def remove(eoriAction: String, eori: Eori, enrolmentKey: EnrolmentKeyType)(implicit
    hc: HeaderCarrier
  ): Future[Either[ErrorMessage, Int]] = {
    val strEnrolmentKey = enrolmentKey.getEnrolmentKey(eori)
    val url = s"${config.taxEnrolmentsServiceUrl}/enrolments/$strEnrolmentKey"
    httpClient.DELETE[HttpResponse](url) map { resp =>
      resp.status match {
        case NO_CONTENT =>
          auditCall(url, eoriAction, eori.toString, strEnrolmentKey)
          Right(NO_CONTENT)
        case failStatus =>
          logger.error(
            s"Remove known facts failed with HTTP status: $failStatus for existing EORI: $eori. Response: ${resp.body}"
          )
          Left(ErrorMessage(s"Remove known facts failed with HTTP status: $failStatus"))
      }
    }
  }

  private def auditCall(url: String, eoriAction: String, eoriNumber: String, enrolmentKey: String)(implicit
    hc: HeaderCarrier
  ): Unit =
    audit.sendExtendedDataEvent(
      transactionName = "Tax-Enrolments-Call",
      path = url,
      details = Json.toJson(RemoveKnownFactsEvent(eoriNumber, enrolmentKey)),
      eventType = s"TaxEnrolmentsRemoveKnownFactsCallFor$eoriAction"
    )
}
