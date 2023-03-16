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
import models.EnrolmentKey.EnrolmentKeyType
import models._
import models.events.ReAllocateGroupEvent
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.{Json, OWrites}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class ReEnrolRequest(userId: String,
                          `type`: String = "principal",
                          action: String = "enrolAndActivate")

object ReEnrolRequest {
  implicit val writes: OWrites[ReEnrolRequest] = Json.writes[ReEnrolRequest]
}

class ReAllocateGroupConnector @Inject()(httpClient: HttpClient, config: AppConfig, audit: Auditable)(implicit ec: ExecutionContext) extends Logging{

  def reAllocate(eori: Eori, enrolmentKey: EnrolmentKeyType, userId: UserId, groupId: GroupId)
                        (implicit hc: HeaderCarrier): Future[Either[ErrorMessage, Int]] = {
    val req = ReEnrolRequest(userId.id)
    val strEnrolmentKey = enrolmentKey.getEnrolmentKey(eori)
    val url = s"${config.taxEnrolmentsServiceUrl}/groups/$groupId/enrolments/$strEnrolmentKey"

    httpClient.POST[ReEnrolRequest, HttpResponse](url, req, Seq("Content-Type" -> "application/json")) map { resp =>
      resp.status match {
        case CREATED =>
          auditCall(url, groupId.toString, userId.toString, strEnrolmentKey)
          Right(CREATED)
        case failStatus =>{
          logger.error(s"Allocate group failed with HTTP status: $failStatus for existing EORI: $eori. Result: ${resp.body}")
          Left(ErrorMessage(s"Allocate group failed with HTTP status: $failStatus (${resp.body})"))
        }
      }
    }
  }

  private def auditCall(url: String, groupId: String, userId: String, enrolmentKey: String)(implicit hc: HeaderCarrier): Unit =
    audit.sendExtendedDataEvent(
      transactionName = "Tax-Enrolments-Call",
      path = url,
      details = Json.toJson(ReAllocateGroupEvent(groupId, userId, enrolmentKey)),
      eventType = s"TaxEnrolmentsReAllocateGroupCall",
    )
}
