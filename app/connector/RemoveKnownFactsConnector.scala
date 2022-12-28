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

import models.EnrolmentKey.EnrolmentKey
import models.{Eori, ErrorMessage}
import play.api.Configuration
import play.api.http.Status._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveKnownFactsConnector @Inject()(httpClient: HttpClient, config: Configuration)(implicit ec: ExecutionContext)
  extends {
    val configuration = config
  } with ContextBuilder {

  private def remove(url: String, service: String)(
    implicit hc: HeaderCarrier): Future[Either[ErrorMessage, Int]] = {
    httpClient.DELETE[HttpResponse](url) map {
      _.status match {
        case NO_CONTENT => Right(NO_CONTENT)
        case failStatus =>
          Left(ErrorMessage(s"[$service] Remove known facts failed with HTTP status: $failStatus"))
      }
    }
  }

  def removeWithESP(eori: Eori, enrolmentKey: EnrolmentKey)
                   (implicit hc: HeaderCarrier): Future[Either[ErrorMessage, Int]] = {
    val url = s"$enrolmentStoreProxyServiceBase/enrolment-store/enrolments/${enrolmentKey.getEnrolmentKey(eori)}"
    remove(url, "Enrolment-Store-Proxy")
  }

  def removeWithTE(eori: Eori, enrolmentKey: EnrolmentKey)
                  (implicit hc: HeaderCarrier): Future[Either[ErrorMessage, Int]] = {
    val url = s"$taxEnrolmentsServiceBase/enrolments/${enrolmentKey.getEnrolmentKey(eori)}"
    remove(url, "Tax-Enrolments")
  }
}
