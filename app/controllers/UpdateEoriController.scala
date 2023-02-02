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

package controllers

import models.DateOfEstablishment.stringToLocalDate
import models.{ConfirmEoriUpdate, EnrolmentKey, Eori, EoriAction, EoriUpdate}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.mvc._
import service.EnrolmentService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.{ConfirmEoriUpdateView, UpdateEoriProblemView, UpdateEoriView}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
case class UpdateEoriController @Inject()(mcc: MessagesControllerComponents,
                                          viewUpdateEori: UpdateEoriView,
                                          viewConfirmUpdate: ConfirmEoriUpdateView,
                                          viewUpdateEoriProblem: UpdateEoriProblemView,
                                          auth: AuthAction,
                                          enrolmentService: EnrolmentService
                                         )(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
    with I18nSupport {

  val formEoriUpdate = Form(
    mapping(
      "existing-eori" -> text(),
      "date-of-establishment-day" -> text(),
      "date-of-establishment-month" -> text(),
      "date-of-establishment-year" -> text(),
      "new-eori" -> text()
    )(EoriUpdate.apply)(EoriUpdate.unapply))

  val formEoriUpdateConfirmation = Form(
    mapping(
      "existing-eori" -> text(),
      "date-of-establishment" -> text(),
      "new-eori" -> text(),
      "enrolment-list" -> text(),
      "confirm" -> boolean
    )(ConfirmEoriUpdate.apply)(ConfirmEoriUpdate.unapply))

  def showPage = auth { implicit request =>
    Ok(viewUpdateEori(formEoriUpdate))
  }

  def continueUpdateEori = auth { implicit request =>
    formEoriUpdate.bindFromRequest.fold(
      _ => Ok(viewUpdateEori(formEoriUpdate)),
      eoriUpdate =>
        Redirect(controllers.routes.UpdateEoriController.showConfirmUpdatePage(eoriUpdate.existingEori, eoriUpdate.dateOfEstablishment, eoriUpdate.newEori))
    )
  }

  def showConfirmUpdatePage(oldEoriNumber: String, establishmentDate: String, newEoriNumber: String) = auth.async { implicit request =>
    enrolmentService.getEnrolments(Eori(oldEoriNumber), stringToLocalDate(establishmentDate))
      .map(enrolments => {
        val enrolmentList = enrolments.filter(_._2).map(_._1).toList
        Ok(viewConfirmUpdate(
          formEoriUpdateConfirmation.fill(ConfirmEoriUpdate(oldEoriNumber, establishmentDate, newEoriNumber, enrolmentList.mkString(","), false)),
          enrolmentList
        ))
      })
  }

  def confirmUpdateEori = auth.async { implicit request =>
    formEoriUpdateConfirmation.bindFromRequest.fold(
      _ => {
        Future(Redirect(controllers.routes.UpdateEoriController.showPage))
      },
      confirmEoriUpdate => {
        if (confirmEoriUpdate.isConfirmed) {
          val updateAllEnrolments = Future.sequence(
            confirmEoriUpdate.enrolmentList.split(",")
              .toList
              .map(EnrolmentKey.getEnrolmentKey(_).get)
              .map(enrolment =>
                enrolmentService.update(
                  Eori(confirmEoriUpdate.existingEori),
                  stringToLocalDate(confirmEoriUpdate.dateOfEstablishment),
                  Eori(confirmEoriUpdate.newEori),
                  enrolment
                ).map(enrolment.serviceName -> _)
              )
          )
          updateAllEnrolments.map { updates => {
            val status = updates.map(either => either._1 -> either._2.isRight).toMap
            if (status.exists(_._2 == false)) {
              Ok(viewUpdateEoriProblem(status.filter(_._2 == true).keys.toList, status.filter(_._2 == false).keys.toList, confirmEoriUpdate.newEori))
            } else {
              Redirect(controllers.routes.EoriActionController.showPageOnSuccess(EoriAction.UPDATE_EORI.toString , confirmEoriUpdate.existingEori, confirmEoriUpdate.newEori))
            }
          }
          }
        } else {
          Future(Redirect(controllers.routes.UpdateEoriController.showPage))
        }
      }
    )
  }

}
