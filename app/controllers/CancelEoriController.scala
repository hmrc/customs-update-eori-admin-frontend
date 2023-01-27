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
import models.{ConfirmEoriCancel, Eori, EoriCancel}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.mvc._
import service.EnrolmentService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.{CancelEoriView, ConfirmCancelEoriView}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
case class CancelEoriController @Inject()(mcc: MessagesControllerComponents,
                                          cancelEoriView: CancelEoriView,
                                          viewConfirmCancelEori: ConfirmCancelEoriView,
                                          auth: AuthAction,
                                          enrolmentService: EnrolmentService
                                         )(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
    with I18nSupport {

  val formCancelEori = Form(
    mapping(
      "existing-eori" -> text(),
      "date-of-establishment-day" -> text(),
      "date-of-establishment-month" -> text(),
      "date-of-establishment-year" -> text()
    )(EoriCancel.apply)(EoriCancel.unapply))

  val formConfirmCancelEori = Form(
    mapping(
      "existing-eori" -> text(),
      "date-of-establishment" -> text(),
      "enrolment-list" -> text(),
      "confirm" -> boolean
    )(ConfirmEoriCancel.apply)(ConfirmEoriCancel.unapply))

  def showPage = auth { implicit request =>
    Ok(cancelEoriView(formCancelEori))
  }

  def continueCancelEori = auth { implicit request =>
    formConfirmCancelEori.bindFromRequest.fold(
      _ => Ok(viewConfirmCancelEori(formConfirmCancelEori)),
      cancelSubscription => Redirect(controllers.routes.CancelEoriController.showConfirmCancel(cancelSubscription.existingEori, cancelSubscription.dateOfEstablishment))
    )
  }

  def showConfirmCancel(existingEori: String, establishmentDate: String) = auth.async { implicit request =>
    enrolmentService.getEnrolments(Eori(existingEori), stringToLocalDate(establishmentDate))
      .map(enrolments => {
        val enrolmentList = enrolments.filter(_._2).map(_._1).toList
        Ok(viewConfirmCancelEori(
          formConfirmCancelEori.fill(ConfirmEoriCancel(existingEori, establishmentDate, enrolmentList.mkString(","), false)),
        ))
      })
  }

}
