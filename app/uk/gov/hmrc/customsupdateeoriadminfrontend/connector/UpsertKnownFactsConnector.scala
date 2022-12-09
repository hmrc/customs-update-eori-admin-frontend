/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.customsupdateeoriadminfrontend.connector

import play.api.Configuration
import play.api.http.Status.NO_CONTENT
import play.api.libs.json.{Json, OWrites}
import uk.gov.hmrc.customsupdateeoriadminfrontend.models.{Enrolment, EnrolmentKey, Eori, ErrorMessage, KeyValue}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class UpsertKnownFactsRequest(verifiers: Seq[KeyValue])

object UpsertKnownFactsRequest {
  implicit val kvWrites: OWrites[KeyValue] = Json.writes[KeyValue]
  implicit val kfrWrites: OWrites[UpsertKnownFactsRequest] =
    Json.writes[UpsertKnownFactsRequest]

  def apply(e: Enrolment): UpsertKnownFactsRequest =
    UpsertKnownFactsRequest(e.verifiers)
}

class UpsertKnownFactsConnector @Inject()(
    httpClient: HttpClient,
    config: Configuration)(implicit ec: ExecutionContext)
    extends {
  val configuration = config
} with ContextBuilder {

  def upsertKnownFacts(eori: Eori, enrolment: Enrolment)(
      implicit hc: HeaderCarrier): Future[Either[ErrorMessage, Int]] = {
    val url =
      s"$enrolmentServiceBaseContext/enrolment-store/enrolments/${EnrolmentKey(eori)}"
    httpClient.PUT[UpsertKnownFactsRequest, HttpResponse](
      url,
      UpsertKnownFactsRequest(enrolment)) map {
      _.status match {
        case NO_CONTENT => Right(NO_CONTENT)
        case failStatus =>
          Left(ErrorMessage(s"Upsert failed with HTTP status: $failStatus"))
      }
    }
  }
}
