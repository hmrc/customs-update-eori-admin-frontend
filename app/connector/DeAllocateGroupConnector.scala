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

import audit.Auditable
import config.AppConfig
import models.EnrolmentKey._
import models._
import models.events.DeAllocateGroupEvent
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeAllocateGroupConnector @Inject() (httpClient: HttpClient, config: AppConfig, audit: Auditable)(implicit
  ec: ExecutionContext
) extends Logging {

  /** ES9 api call to TES - delete an enrolment or known fact
    * @param eori
    * @param enrolmentKey
    * @param groupId
    * @param hc
    * @return
    */
  def deAllocateGroup(eoriAction: String, eori: Eori, enrolmentKey: EnrolmentKeyType, groupId: GroupId)(implicit
    hc: HeaderCarrier
  ): Future[Either[ErrorMessage, Int]] = {
    val strEnrolmentKey = enrolmentKey.getEnrolmentKey(eori)
    val url = s"${config.taxEnrolmentsServiceUrl}/groups/$groupId/enrolments/$strEnrolmentKey"
    httpClient.DELETE[HttpResponse](url) map { resp =>
      resp.status match {
        case NO_CONTENT =>
          auditCall(url, eoriAction, groupId.toString, strEnrolmentKey)
          Right(NO_CONTENT)
        case failStatus =>
          logger.error(
            s"Delete enrolment failed with HTTP status: $failStatus for existing EORI: $eori. Response: ${resp.body}"
          )
          Left(ErrorMessage(s"Delete enrolment failed with HTTP status: $failStatus"))
      }
    }
  }

  private def auditCall(url: String, eoriAction: String, groupId: String, enrolmentKey: String)(implicit
    hc: HeaderCarrier
  ): Unit =
    audit.sendExtendedDataEvent(
      transactionName = "Tax-Enrolments-Call",
      path = url,
      details = Json.toJson(DeAllocateGroupEvent(groupId, enrolmentKey)),
      eventType = s"TaxEnrolmentsDeAllocateGroupCallFor$eoriAction"
    )
}
