/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.customsupdateeoriadminfrontend.connector

import play.api.Configuration
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.customsupdateeoriadminfrontend.models.{EnrolmentKey, Eori, ErrorMessage, UserId}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class Users(principalUserIds: Seq[String], delegatedUserIds: Seq[String])

object Users {
  implicit val reads: Reads[Users] = Json.reads[Users]
}

class QueryUsersConnector @Inject()(
    httpClient: HttpClient,
    config: Configuration)(implicit ec: ExecutionContext)
    extends {
  val configuration = config
} with ContextBuilder {

  def queryUsers(eori: Eori)(
      implicit hc: HeaderCarrier): Future[Either[ErrorMessage, UserId]] = {
    val url =
      s"$enrolmentServiceBaseContext/enrolment-store/enrolments/${EnrolmentKey(eori)}/users"
    httpClient.GET[Either[UpstreamErrorResponse, Users]](url).map { users =>
      users.map { gs =>
        Right(UserId(gs.principalUserIds.head))
      } getOrElse Left(
        ErrorMessage(s"Could not find User for existing EORI: $eori"))
    }
  }
}
