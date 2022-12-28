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

package service

import cats.data.EitherT
import connector._
import models.EnrolmentKey._
import models.{Enrolment, Eori, ErrorMessage}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EnrolmentService @Inject()(groupsConnector: QueryGroupsConnector,
                                 usersConnector: connector.QueryUsersConnector,
                                 knownFactsConnector: QueryKnownFactsConnector,
                                 upsertKnownFactsConnector: UpsertKnownFactsConnector,
                                 deAllocateGroupConnector: DeAllocateGroupConnector,
                                 reAllocateGroupConnector: ReAllocateGroupConnector,
                                 removeKnownFactsConnector: RemoveKnownFactsConnector
                                )(implicit ec: ExecutionContext) {

  def updateWithESP(existingEori: Eori, date: LocalDate, newEori: Eori)
                   (implicit hc: HeaderCarrier): Future[Either[ErrorMessage, Enrolment]] = {
    val queryGroups = groupsConnector.query(existingEori, HMRC_CUS_ORG)
    val queryUsers = usersConnector.query(existingEori, HMRC_CUS_ORG)
    val queryKnownFacts = knownFactsConnector.query(existingEori, HMRC_CUS_ORG, date)

    val result = for {
      enrolment <- EitherT(queryKnownFacts) // ES20
      userId <- EitherT(queryUsers) // ES0
      groupId <- EitherT(queryGroups) // ES1
      _ <- EitherT(upsertKnownFactsConnector.upsertWithESP(newEori, HMRC_CUS_ORG, enrolment)) // ES6
      _ <- EitherT(deAllocateGroupConnector.deAllocateWithESP(existingEori, HMRC_CUS_ORG, groupId)) // ES9
      _ <- EitherT(reAllocateGroupConnector.reAllocateWithESP(newEori, HMRC_CUS_ORG, userId, groupId)) // ES8
      _ <- EitherT(removeKnownFactsConnector.removeWithESP(existingEori, HMRC_CUS_ORG)) // ES7

    } yield enrolment;
    result.value
  }
}
