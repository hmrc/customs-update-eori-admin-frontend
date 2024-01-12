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
import models.{Eori, ErrorMessage}
import play.api.Logging
import play.api.http.MimeTypes.JSON
import play.api.http.Status.{NOT_FOUND, NO_CONTENT}
import play.api.libs.json.Json
import play.mvc.Http.HeaderNames.CONTENT_TYPE
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CustomsDataStoreConnector @Inject() (httpClient: HttpClient, config: AppConfig, audit: Auditable)(implicit
  ec: ExecutionContext
) extends Logging {

  def notify(newEori: Eori)(implicit hc: HeaderCarrier): Future[Either[ErrorMessage, Int]] = {
    val contentType = CONTENT_TYPE -> JSON

    httpClient
      .POST[Eori, HttpResponse](config.customsDataStoreUrl, newEori, Seq(contentType))
      .map { response =>
        auditCall(config.customsDataStoreUrl, newEori.toString, response)
        response.status match {
          case NO_CONTENT =>
            logger.info(s"[CDS] No content for EORI: $newEori")
            Right(NO_CONTENT)
          case NOT_FOUND =>
            logger.info(s"[CDS] Not found notification for EORI: $newEori")
            Right(NOT_FOUND)
          case failStatus =>
            logger.error(s"Notification failed with HTTP status: $failStatus for EORI: $newEori")
            Left(ErrorMessage(s"Notification failed with HTTP status: $failStatus"))
        }
      }
  }

  private def auditCall(url: String, newEori: String, response: HttpResponse)(implicit hc: HeaderCarrier): Unit =
    audit.sendExtendedDataEvent(
      transactionName = "Custom-Data-Store-Call",
      path = url,
      details = Json.toJson(
        Map(
          "request"  -> Map("newEori" -> newEori),
          "response" -> Map("status" -> s"${response.status}", "message" -> s"${response.body}")
        )
      ),
      eventType = "CustomDataStoreCall"
    )
}
