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

import models.EoriEventEnum.UPDATE
import models.ValidateEori.{ESTABLISHMENT_DATE_WRONG, FALSE, TRUE}
import models._
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BulkUpdateService @Inject()(enrolmentService: EnrolmentService)(implicit ec: ExecutionContext, hc: HeaderCarrier) extends Logging {

  case class EoriValidationError(eori: String, badEnrolments: List[(String, ValidateEori.Value)])

  case class EoriValidationSuccess(eoriUpdate: EoriUpdate, enrolments: Seq[(String, ValidateEori.Value)])

  private def validate(eoriUpdate: EoriUpdate): Future[Either[EoriValidationError, EoriValidationSuccess]] = {
    enrolmentService
      .getEnrolments(UPDATE, Eori(eoriUpdate.existingEori), eoriUpdate.dateOfEstablishment)
      .map { enrolments =>
        val badEnrolments = enrolments.filter(_._2 != TRUE).toList

        if (badEnrolments.nonEmpty) {
          Left(EoriValidationError(eoriUpdate.existingEori, badEnrolments))
        } else {
          Right(EoriValidationSuccess(eoriUpdate, enrolments))
        }
      }
  }

  private def getValidationErrors(eoriValidations: Seq[Either[EoriValidationError, EoriValidationSuccess]]): Seq[EoriValidationError] =
    eoriValidations.collect { case Left(eoriValidationError) => eoriValidationError }

  private def getValidationSuccesses(eoriValidations: Seq[Either[EoriValidationError, EoriValidationSuccess]]): Seq[EoriValidationSuccess] =
    eoriValidations.collect { case Right(success) => success }

  private def getUpdatableEORIsWithEnrolments(eoriValidationSuccesses: Seq[EoriValidationSuccess]): Seq[EoriValidationSuccess] =
    eoriValidationSuccesses.foldLeft(Seq.empty[EoriValidationSuccess]) { (acc, success) =>
      val filteredSuccess = success.copy(enrolments = success.enrolments.filter { case (enrolment, _) =>
        UpdatableEnrolments.values.contains(enrolment)
      })
      acc :+ filteredSuccess
    }

  private def getEORIsWithNonUpdatableEnrolments(eoriValidationSuccesses: Seq[EoriValidationSuccess]): Seq[EoriValidationSuccess] =
    eoriValidationSuccesses.foldLeft(Seq.empty[EoriValidationSuccess]) { (acc, success) =>
      val filteredSuccess = success.copy(enrolments = success.enrolments.filter { case (enrolment, _) =>
        !UpdatableEnrolments.values.contains(enrolment)
      })
      acc :+ filteredSuccess
    }

  def updateAll(eoriUpdates: Seq[EoriUpdate]): Future[Seq[Either[ErrorMessage, Enrolment]]] = {
    for {
      validations <- Future.sequence(eoriUpdates.map(validate))
      errors <- Future.successful(getValidationErrors(validations))
      successes <- Future.successful(getValidationSuccesses(validations))
      updatable <- Future.successful(getUpdatableEORIsWithEnrolments(successes))
      nonUpdatable <- Future.successful(getEORIsWithNonUpdatableEnrolments(successes))
      update <- Future.sequence(updatable.map {
        success =>
          Future.sequence(success.enrolments.flatMap { case (enrolment, _) =>
            EnrolmentKey.getEnrolmentKey(enrolment).map { enrolmentKey =>
              enrolmentService.update(Eori(success.eoriUpdate.existingEori), success.eoriUpdate.dateOfEstablishment, Eori(success.eoriUpdate.newEori), enrolmentKey)
            }
          })
      })
    } yield update.flatten
  }
}






