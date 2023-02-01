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

package service

import cats.data.EitherT
import connector._
import models.EnrolmentKey._
import models.{Enrolment, EnrolmentKey, Eori, ErrorMessage}
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
                                 removeKnownFactsConnector: RemoveKnownFactsConnector,
                                 customsDataStoreConnector: CustomsDataStoreConnector
                                )(implicit ec: ExecutionContext) {

  def getEnrolments(existingEori: Eori, date: LocalDate)(implicit hc: HeaderCarrier) = {
    Future.sequence(
      EnrolmentKey.values.toSeq.map(enrolmentKey => {
        val checkEnrolments = for {
          enrolmentResult <- knownFactsConnector.query(existingEori, enrolmentKey, date)
            .map(knowFacts => knowFacts.isRight)
            .recover(_ => false)
          userResult <- if (enrolmentResult) usersConnector.query(existingEori, enrolmentKey)
            .map(user => user.isRight)
            .recover(_ => false) else Future.successful(false)
          finalResult <- if (userResult) groupsConnector.query(existingEori, enrolmentKey)
            .map(group => group.isRight)
            .recover(_ => false) else Future.successful(false)
        } yield finalResult

        checkEnrolments.map(check => enrolmentKey.serviceName -> check)
      })
    )
  }

  def update(existingEori: Eori, date: LocalDate, newEori: Eori, enrolmentKey: EnrolmentKeyType)
            (implicit hc: HeaderCarrier): Future[Either[ErrorMessage, Enrolment]] = {
    val queryGroups = groupsConnector.query(existingEori, enrolmentKey)
    val queryUsers = usersConnector.query(existingEori, enrolmentKey)
    val queryKnownFacts = knownFactsConnector.query(existingEori, enrolmentKey, date)

    val result = for {
      enrolment <- EitherT(queryKnownFacts) // ES20
      userId <- EitherT(queryUsers) // ES0
      groupId <- EitherT(queryGroups) // ES1
      _ <- EitherT(upsertKnownFactsConnector.upsert(newEori, enrolmentKey, enrolment)) // ES6
      _ <- EitherT(deAllocateGroupConnector.deAllocateGroup(existingEori, enrolmentKey, groupId)) // ES9
      _ <- EitherT(removeKnownFactsConnector.remove(existingEori, enrolmentKey)) // ES7
      _ <- EitherT(reAllocateGroupConnector.reAllocate(newEori, enrolmentKey, userId, groupId)) // ES8
      _ <- EitherT(customsDataStoreConnector.notify(newEori)) // Notify Customs Data Store with new number
    } yield enrolment
    result.value
  }

  /**
   * @param existingEori
   * @param date
   * @param enrolmentKey
   * @param hc
   * @return
   */

  def cancel(existingEori: Eori, date: LocalDate, enrolmentKey: EnrolmentKeyType)
            (implicit hc: HeaderCarrier): Future[Either[ErrorMessage, Enrolment]] = {
    val queryGroups = groupsConnector.query(existingEori, enrolmentKey)
    val queryKnownFacts = knownFactsConnector.query(existingEori, enrolmentKey, date)

    val result = for {
      enrolment <- EitherT(queryKnownFacts) // ES20
      groupId <- EitherT(queryGroups) // ES1
      _ <- EitherT(deAllocateGroupConnector.deAllocateGroup(existingEori, enrolmentKey, groupId)) // ES9
      _ <- EitherT(removeKnownFactsConnector.remove(existingEori, enrolmentKey)) // ES7
    } yield enrolment
    result.value
  }
}
