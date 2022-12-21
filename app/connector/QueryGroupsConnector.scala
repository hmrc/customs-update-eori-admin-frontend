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

import models.{EnrolmentKey, Eori, ErrorMessage, GroupId}
import play.api.Configuration
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class Groups(principalGroupIds: Seq[String],
                  delegatedGroupIds: Seq[String])

object Groups {
  implicit val reads: Reads[Groups] = Json.reads[Groups]
}

class QueryGroupsConnector @Inject()(
    httpClient: HttpClient,
    config: Configuration)(implicit ec: ExecutionContext)
    extends {
  val configuration = config
} with ContextBuilder {

  def queryGroups(eori: Eori)(
      implicit hc: HeaderCarrier): Future[Either[ErrorMessage, GroupId]] = {
    val url =
      s"$enrolmentServiceBaseContext/enrolment-store/enrolments/${EnrolmentKey(eori)}/groups"
    httpClient.GET[Either[UpstreamErrorResponse, Groups]](url).map { groups =>
      groups.map { gs =>
        Right(GroupId(gs.principalGroupIds.head))
      } getOrElse Left(
        ErrorMessage(s"Could not find Group for existing EORI: $eori"))
    }
  }
}
