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

import mappings.Mappings
import models.ValidateEori.{ESTABLISHMENT_DATE_WRONG, TRUE}
import models._
import play.api.data.Forms._
import play.api.data.{Form, FormError, Forms}
import play.api.i18n.{I18nSupport, Lang}
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
  extends FrontendController(mcc) with Mappings
    with I18nSupport {

  private val SPLITTER_CHARACTER = ","

  val formEoriUpdate = Form(
    mapping(
      "existing-eori" -> eoriNumber(
        "eori.validation.existingEori.required",
        "eori.validation.existingEori.format"
      ),
      "date-of-establishment" -> localDate(
        invalidKey = "eori.validation.establishmentDate.invalid",
        threeDateComponentsMissingKey = "eori.validation.establishmentDate.required.all",
        twoDateComponentsMissingKey = "eori.validation.establishmentDate.required.two",
        oneDateComponentMissingKey = "eori.validation.establishmentDate.required.one",
        mustBeInPastKey = "eori.validation.establishmentDate.mustBeInPast",
      ),
      "new-eori" -> eoriNumber(
        "eori.validation.newEori.required",
        "eori.validation.newEori.format"
      ),
    )(EoriUpdate.apply)(EoriUpdate.unapply))

  val formEoriUpdateConfirmation = Form(
    mapping(
      "existing-eori" -> text(),
      "date-of-establishment" -> Forms.localDate(LocalDateBinder.dateTimePattern),
      "new-eori" -> text(),
      "enrolment-list" -> text(),
      "not-updatable-enrolment-list" -> text()
    )(ConfirmEoriUpdate.apply)(ConfirmEoriUpdate.unapply))

  def showPage = auth { implicit request =>
    Ok(viewUpdateEori(formEoriUpdate))
  }

  def continueUpdateEori = auth.async { implicit request =>
    formEoriUpdate.bindFromRequest().fold(
      formWithError => Future(BadRequest(viewUpdateEori(formWithError))),
      eoriUpdate =>
        enrolmentService.getEnrolments(Eori(eoriUpdate.existingEori), eoriUpdate.dateOfEstablishment)
          .map(enrolments => {
            if (enrolments.exists(_._2 == ESTABLISHMENT_DATE_WRONG)) {
              BadRequest(viewUpdateEori(formEoriUpdate.fill(eoriUpdate).withError(FormError("date-of-establishment", mcc.messagesApi.apply("eori.validation.establishmentDate.mustBeMatched")(Lang("en"))))))
            } else {
              val enrolmentList = enrolments.filter(_._2 == TRUE).map(_._1).toList
              val updatableEnrolments = enrolmentList.filter(e => UpdatableEnrolments.values.contains(e))
              val notUpdatableEnrolments = enrolmentList.filter(e => !UpdatableEnrolments.values.contains(e))
              Ok(viewConfirmUpdate(
                formEoriUpdateConfirmation.fill(ConfirmEoriUpdate(eoriUpdate.existingEori, eoriUpdate.dateOfEstablishment, eoriUpdate.newEori, updatableEnrolments.mkString(SPLITTER_CHARACTER), notUpdatableEnrolments.mkString(SPLITTER_CHARACTER))),
                updatableEnrolments,
                notUpdatableEnrolments
              ))
            }
          })
    )
  }

  def confirmUpdateEori = auth.async { implicit request =>
    formEoriUpdateConfirmation.bindFromRequest().fold(
      _ => {
        Future(Redirect(controllers.routes.UpdateEoriController.showPage))
      },
      confirmEoriUpdate => {
        val updateAllEnrolments = Future.sequence(
          confirmEoriUpdate.enrolmentList.split(SPLITTER_CHARACTER)
            .toList
            .map(EnrolmentKey.getEnrolmentKey(_).get)
            .map(enrolment =>
              enrolmentService.update(
                Eori(confirmEoriUpdate.existingEori),
                confirmEoriUpdate.dateOfEstablishment,
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
            Redirect(controllers.routes.EoriActionController.showPageOnSuccess(
              cancelOrUpdate = Some(EoriActionEnum.UPDATE_EORI.toString),
              oldEoriNumber = Some(confirmEoriUpdate.existingEori),
              newEoriNumber = Some(confirmEoriUpdate.newEori),
              subscribedEnrolments = Some(confirmEoriUpdate.enrolmentList),
              notUpdatableEnrolments = Some(confirmEoriUpdate.notUpdatableEnrolmentList))
            )
          }
        }
        }
      }
    )
  }

}
