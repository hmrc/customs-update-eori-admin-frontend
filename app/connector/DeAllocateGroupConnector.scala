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
import EnrolmentKey._
import play.api.Configuration
import play.api.http.Status._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeAllocateGroupConnector @Inject()(httpClient: HttpClient, config: Configuration)(implicit ec: ExecutionContext)
  extends {
    val configuration = config
  } with ContextBuilder {

  private def deAllocateGroup(url: String, service: String)(implicit hc: HeaderCarrier): Future[Either[ErrorMessage, Int]] = {
    httpClient.DELETE[HttpResponse](url) map {
      _.status match {
        case NO_CONTENT => Right(NO_CONTENT)
        case failStatus =>
          Left(ErrorMessage(s"[$service] Delete enrolment failed with HTTP status: $failStatus"))
      }
    }
  }

  def deAllocateWithESP(eori: Eori, enrolmentKey: EnrolmentKey, groupId: GroupId)
                       (implicit hc: HeaderCarrier): Future[Either[ErrorMessage, Int]] = {
    deAllocateGroup(
      s"$enrolmentStoreProxyServiceBase/enrolment-store/groups/$groupId/enrolments/${enrolmentKey.getEnrolmentKey(eori)}",
      "Enrolment-Store-Proxy"
    )
  }

  def deAllocateGroupWithTE(eori: Eori, enrolmentKey: EnrolmentKey, groupId: GroupId)
                           (implicit hc: HeaderCarrier): Future[Either[ErrorMessage, Int]] = {
    deAllocateGroup(
      s"$taxEnrolmentsServiceBase/groups/$groupId/enrolments/${enrolmentKey.getEnrolmentKey(eori)}",
      "Tax-Enrolments"
    )
  }
}
