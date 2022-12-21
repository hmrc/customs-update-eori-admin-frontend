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
import cats.instances.future._
import connector._
import models.{Enrolment, Eori, ErrorMessage}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EnrolmentService @Inject()(
    qg: QueryGroupsConnector,
    qu: QueryUsersConnector,
    qkf: QueryKnownFactsConnector)(implicit ec: ExecutionContext) {

  def update(existingEori: Eori, date: LocalDate, newEori: Eori)(
      implicit hc: HeaderCarrier): Future[Either[ErrorMessage, Enrolment]] = {

    val queryGroups = qg.queryGroups(existingEori)
    val queryUsers = qu.queryUsers(existingEori)
    val queryKnownFacts = qkf.queryKnownFacts(existingEori, date)

    val f = for {
      enrolment <- EitherT(queryKnownFacts) //ES20
      userId <- EitherT(queryUsers) //ES0
      groupId <- EitherT(queryGroups) //ES1

    } yield enrolment;
    f.value
  }
}
