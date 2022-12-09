/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.customsupdateeoriadminfrontend.connector

import play.api.Configuration
import play.api.http.Status._
import play.api.libs.json.{Json, OWrites}
import uk.gov.hmrc.customsupdateeoriadminfrontend.models.{EnrolmentKey, Eori, ErrorMessage, GroupId, UserId}
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

class ReEnrolConnector @Inject()(httpClient: HttpClient, config: Configuration)(
    implicit ec: ExecutionContext)
    extends {
  val configuration = config
} with ContextBuilder {

  def reEnrol(eori: Eori, userId: UserId, groupId: GroupId)(
      implicit hc: HeaderCarrier): Future[Either[ErrorMessage, Int]] = {
    val url =
      s"$enrolmentServiceBaseContext/enrolment-store/groups/$groupId/enrolments/${EnrolmentKey(eori)}"
    val req = ReEnrolRequest(userId.id)
    httpClient.POST[ReEnrolRequest, HttpResponse](
      url,
      req,
      Seq("Content-Type" -> "application/json")) map { resp =>
      resp.status match {
        case CREATED => Right(CREATED)
        case failStatus =>
          Left(
            ErrorMessage(
              s"Re-enrol failed with HTTP status: $failStatus (${resp.body})"))
      }
    }
  }
}
