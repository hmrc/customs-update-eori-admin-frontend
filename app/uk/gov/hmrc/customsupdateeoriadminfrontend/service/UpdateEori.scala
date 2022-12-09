/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.customsupdateeoriadminfrontend.service

import uk.gov.hmrc.customsupdateeoriadminfrontend.connector.{CustomsDataStoreConnector, DeEnrolConnector, QueryGroupsConnector, QueryKnownFactsConnector, QueryUsersConnector, ReEnrolConnector, RemoveKnownFactsConnector, UpsertKnownFactsConnector}
import uk.gov.hmrc.customsupdateeoriadminfrontend.models.{Eori, ErrorMessage}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpdateEori @Inject()(
    qg: QueryGroupsConnector,
    qu: QueryUsersConnector,
    qkf: QueryKnownFactsConnector,
    ukf: UpsertKnownFactsConnector,
    de: DeEnrolConnector,
    rkf: RemoveKnownFactsConnector,
    re: ReEnrolConnector,
    cds: CustomsDataStoreConnector)(implicit ec: ExecutionContext) {

  def update(existingEori: Eori, date: LocalDate, newEori: Eori)(
      implicit hc: HeaderCarrier) = {
    val queryGroups = qg.queryGroups(existingEori)
    val queryUsers = qu.queryUsers(existingEori)
    val queryKnownFacts = qkf.queryKnownFacts(existingEori, date)


  }
}
