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
import models.{Eori, ErrorMessage, UserId}
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class Users(principalUserIds: Seq[String], delegatedUserIds: Seq[String])

object Users {
  implicit val reads: Reads[Users] = Json.reads[Users]
}

class QueryUsersConnector @Inject()(httpClient: HttpClient, config: AppConfig)(implicit ec: ExecutionContext) {

  def query(eori: Eori, enrolmentKey: EnrolmentKeyType)(implicit hc: HeaderCarrier): Future[Either[ErrorMessage, UserId]] = {
    val url = s"${config.enrolmentStoreProxyServiceUrl}/enrolment-store/enrolments/${enrolmentKey.getEnrolmentKey(eori)}/users"
    httpClient.GET[Either[UpstreamErrorResponse, Users]](url).map { users =>
      users.map { gs =>
        Right(UserId(gs.principalUserIds.head))
      } getOrElse Left(
        ErrorMessage(s"Could not find User for existing EORI: $eori"))
    }
  }
}
