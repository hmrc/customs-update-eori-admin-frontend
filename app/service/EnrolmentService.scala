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
import models.EoriEventEnum.{CANCEL, UPDATE}
import models._
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

  private val ERROR_MESSAGE_PART = "date you have entered does not match"

  def getEnrolments(eoriAction: String, existingEori: Eori, date: LocalDate)(implicit hc: HeaderCarrier) = {
    Future.sequence(
      EnrolmentKey.values.toSeq.map(enrolmentKey => {
        val checkEnrolments = for {
          enrolmentResult <- knownFactsConnector.query(eoriAction, existingEori, enrolmentKey, date)
            .map(knowFacts =>
              if (knowFacts.isRight) ValidateEori.TRUE
              else {
                if (knowFacts.left.getOrElse(ErrorMessage("UNKNOWN")).message.contains(ERROR_MESSAGE_PART)) ValidateEori.ESTABLISHMENT_DATE_WRONG
                else ValidateEori.FALSE
              }
            )
            .recover(_ => ValidateEori.FALSE)
          userResult <-
            if (enrolmentResult.equals(ValidateEori.TRUE))
              usersConnector.query(existingEori, enrolmentKey)
                .map(user =>
                  if (user.isRight) ValidateEori.TRUE
                  else ValidateEori.FALSE
                )
                .recover(_ => ValidateEori.FALSE)
            else
              Future.successful(enrolmentResult)

          finalResult <-
            if (enrolmentResult.equals(ValidateEori.TRUE) && userResult.equals(ValidateEori.TRUE))
              groupsConnector.query(existingEori, enrolmentKey)
                .map(group =>
                  if (group.isRight) ValidateEori.TRUE
                  else ValidateEori.FALSE
                )
                .recover(_ => ValidateEori.FALSE)
            else if (enrolmentResult == ValidateEori.ESTABLISHMENT_DATE_WRONG)
              Future.successful(enrolmentResult)
            else
              Future.successful(ValidateEori.FALSE)
        } yield finalResult

        checkEnrolments.map(check => enrolmentKey.serviceName -> check)
      })
    )
  }

  def update(existingEori: Eori, date: LocalDate, newEori: Eori, enrolmentKey: EnrolmentKeyType)
            (implicit hc: HeaderCarrier): Future[Either[ErrorMessage, Enrolment]] = {
    val queryGroups = groupsConnector.query(existingEori, enrolmentKey)
    val queryUsers = usersConnector.query(existingEori, enrolmentKey)
    val queryKnownFacts = knownFactsConnector.query(UPDATE, existingEori, enrolmentKey, date)

    val result = for {
      enrolment <- EitherT(queryKnownFacts) // ES20
      userId <- EitherT(queryUsers) // ES0
      groupId <- EitherT(queryGroups) // ES1
      _ <- EitherT(upsertKnownFactsConnector.upsert(newEori, enrolmentKey, enrolment)) // ES6
      _ <- EitherT(deAllocateGroupConnector.deAllocateGroup(UPDATE, existingEori, enrolmentKey, groupId)) // ES9
      _ <- EitherT(removeKnownFactsConnector.remove(UPDATE, existingEori, enrolmentKey)) // ES7
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
    val queryKnownFacts = knownFactsConnector.query(CANCEL, existingEori, enrolmentKey, date)

    val result = for {
      enrolment <- EitherT(queryKnownFacts) // ES20
      groupId <- EitherT(queryGroups) // ES1
      _ <- EitherT(deAllocateGroupConnector.deAllocateGroup(CANCEL, existingEori, enrolmentKey, groupId)) // ES9
      _ <- EitherT(removeKnownFactsConnector.remove(CANCEL, existingEori, enrolmentKey)) // ES7
    } yield enrolment
    result.value
  }
}
