/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.customsupdateeoriadminfrontend.connector

import play.api.Configuration
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.customsupdateeoriadminfrontend.models.{EnrolmentKey, Eori, ErrorMessage, GroupId}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}

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
