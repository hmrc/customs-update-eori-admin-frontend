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
import models.{Eori, ErrorMessage}
import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveKnownFactsConnector @Inject()(httpClient: HttpClient, config: AppConfig)(implicit ec: ExecutionContext) extends Logging {

  def remove(eori: Eori, enrolmentKey: EnrolmentKeyType)
            (implicit hc: HeaderCarrier): Future[Either[ErrorMessage, Int]] = {
    val url = s"${config.taxEnrolmentsServiceUrl}/enrolments/${enrolmentKey.getEnrolmentKey(eori)}"
    httpClient.DELETE[HttpResponse](url) map { resp =>
      resp.status match {
        case NO_CONTENT => Right(NO_CONTENT)
        case failStatus => {
          logger.error(s"Remove known facts failed with HTTP status: $failStatus for existing EORI: $eori. Response: ${resp.body}")
          Left(ErrorMessage(s"Remove known facts failed with HTTP status: $failStatus"))
        }

      }
    }
  }
}
