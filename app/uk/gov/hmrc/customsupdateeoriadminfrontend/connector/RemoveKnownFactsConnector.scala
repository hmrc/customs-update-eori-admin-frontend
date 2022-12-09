/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.customsupdateeoriadminfrontend.connector

import play.api.Configuration
import play.api.http.Status._
import uk.gov.hmrc.customsupdateeoriadminfrontend.models.{EnrolmentKey, Eori, ErrorMessage}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveKnownFactsConnector @Inject()(
    httpClient: HttpClient,
    config: Configuration)(implicit ec: ExecutionContext)
    extends {
  val configuration = config
} with ContextBuilder {

  def removeKnownFacts(eori: Eori)(
      implicit hc: HeaderCarrier): Future[Either[ErrorMessage, Int]] = {
    val url =
      s"$enrolmentServiceBaseContext/enrolment-store/enrolments/${EnrolmentKey(eori)}"
    httpClient.DELETE[HttpResponse](url) map {
      _.status match {
        case NO_CONTENT => Right(NO_CONTENT)
        case failStatus =>
          Left(
            ErrorMessage(
              s"Remove known facts failed with HTTP status: $failStatus"))
      }
    }
  }
}
