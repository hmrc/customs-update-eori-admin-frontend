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
import models.LocalDateBinder._
import models._
import play.api.data.{Form, Forms}
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.mvc._
import service.EnrolmentService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.{CancelEoriProblemView, CancelEoriView, ConfirmCancelEoriView}

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
case class CancelEoriController @Inject()(mcc: MessagesControllerComponents,
                                          cancelEoriView: CancelEoriView,
                                          viewConfirmCancelEori: ConfirmCancelEoriView,
                                          cancelEoriProblemView: CancelEoriProblemView,
                                          auth: AuthAction,
                                          enrolmentService: EnrolmentService
                                         )(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
    with Mappings
    with I18nSupport {

  val formCancelEori = Form(
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
      )
    )(EoriCancel.apply)(EoriCancel.unapply))

  val formConfirmCancelEori = Form(
    mapping(
      "existing-eori" -> text(),
      "date-of-establishment" -> Forms.localDate(LocalDateBinder.dateTimePattern),
      "enrolment-list" -> text()
    )(ConfirmEoriCancel.apply)(ConfirmEoriCancel.unapply))

  def showPage = auth { implicit request =>
    Ok(cancelEoriView(formCancelEori))
  }

  def continueCancelEori = auth { implicit request =>
    formCancelEori.bindFromRequest().fold(
      formWithError => BadRequest(cancelEoriView(formWithError)),
      eoriCancel => Redirect(controllers.routes.CancelEoriController.showConfirmCancelPage(eoriCancel.existingEori, eoriCancel.dateOfEstablishment))
    )
  }

  def showConfirmCancelPage(existingEori: String, establishmentDate: LocalDate) = auth.async { implicit request =>
    enrolmentService.getEnrolments(Eori(existingEori), establishmentDate)
      .map(enrolments => {
        val enrolmentList = enrolments.filter(_._2).map(_._1).toList
        val cancelableEnrolments = enrolmentList.filter(e => CancelableEnrolments.values.contains(e))
        val notCancelableEnrolments = enrolmentList.filter(e => !CancelableEnrolments.values.contains(e))
        Ok(viewConfirmCancelEori(
          formConfirmCancelEori.fill(ConfirmEoriCancel(existingEori, establishmentDate, cancelableEnrolments.mkString(","))),
          cancelableEnrolments,
          notCancelableEnrolments
        ))
      })
  }

  def confirmCancelEori = auth.async { implicit request =>
    formConfirmCancelEori.bindFromRequest().fold(
      _ => {
        Future(Redirect(controllers.routes.CancelEoriController.showPage))
      },
      confirmEoriCancel => {
            val updateAllEnrolments = Future.sequence(
             confirmEoriCancel.enrolmentList.split(",")
               .toList
               .map(EnrolmentKey.getEnrolmentKey(_).get)
               .map(enrolment =>
                 enrolmentService.cancel(
                   Eori(confirmEoriCancel.existingEori),
                   confirmEoriCancel.dateOfEstablishment,
                   enrolment
                 ).map(enrolment.serviceName -> _)
               )
           )
           updateAllEnrolments.map { updates => {
             val status = updates.map(either => either._1 -> either._2.isRight).toMap
             if (status.exists(_._2 == false)) {
               Ok(cancelEoriProblemView(status.filter(_._2 == true).keys.toList, status.filter(_._2 == false).keys.toList))
             } else {
               Redirect(controllers.routes.EoriActionController.showPageOnSuccess(cancelOrUpdate = Some(EoriActionEnum.CANCEL_EORI.toString),oldEoriNumber = Some(confirmEoriCancel.existingEori),cancelledEnrolments = Some(confirmEoriCancel.enrolmentList)))
             }
           }
           }

      }
    )
  }

}
