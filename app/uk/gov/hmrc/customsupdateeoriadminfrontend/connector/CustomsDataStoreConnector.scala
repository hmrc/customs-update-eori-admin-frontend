/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.customsupdateeoriadminfrontend.connector

import play.api.Configuration
import play.api.http.MimeTypes.JSON
import play.api.http.Status.NO_CONTENT
import play.mvc.Http.HeaderNames.CONTENT_TYPE
import uk.gov.hmrc.customsupdateeoriadminfrontend.audit.Auditor
import uk.gov.hmrc.customsupdateeoriadminfrontend.models.{Eori, ErrorMessage}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CustomsDataStoreConnector @Inject()(
    httpClient: HttpClient,
    config: Configuration,
    audit: Auditor)(implicit ec: ExecutionContext)
    extends {
  val configuration = config
} with ContextBuilder {

  def notify(existingEori: Eori)(implicit hc: HeaderCarrier): Future[Either[ErrorMessage, Int]] = {
    val contentType = CONTENT_TYPE -> JSON

    auditRequest(customsDataStoreBaseContext, existingEori.eori)
    
    httpClient.POST[Eori, HttpResponse](customsDataStoreBaseContext, existingEori, Seq(contentType))
      .map { response =>
        auditResponse(response, customsDataStoreBaseContext)
        response.status match {
          case NO_CONTENT => Right(NO_CONTENT)
          case failStatus =>
            Left(
              ErrorMessage(
                s"notification failed with HTTP status: $failStatus"))
        }
      }
  }

  private def auditRequest(url: String, existingEori: String)(
      implicit hc: HeaderCarrier): Unit = {
    audit.sendDataEvent(
      transactionName = "CustomsDataStoreRequestSubmitted",
      path = url,
      detail = Map("existingEori" -> existingEori),
      auditType = "CustomsDataStoreRequest"
    )
  }

  private def auditResponse(response: HttpResponse, url: String)(
      implicit hc: HeaderCarrier): Unit = {
    audit.sendDataEvent(
      transactionName = "CustomsDataStoreResponseReceived",
      path = url,
      detail = Map("status" -> s"${response.status}",
                   "message" -> s"${response.body}"),
      auditType = "CustomsDataStoreResponse"
    )
  }
}
