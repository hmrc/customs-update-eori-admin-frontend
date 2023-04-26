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

import models.{EoriAction, EoriActionEnum}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.EoriActionView
import views.html.EoriOperationSuccessfulView

import javax.inject.{Inject, Singleton}

@Singleton
case class EoriActionController @Inject() (
  mcc: MessagesControllerComponents,
  viewEoriAction: EoriActionView,
  eoriOperationSuccessfulView: EoriOperationSuccessfulView,
  auth: AuthAction
) extends FrontendController(mcc) with I18nSupport {

  val form: Form[EoriAction] = Form(
    mapping(
      "update-or-cancel-eori" -> text()
    )(EoriAction.apply)(EoriAction.unapply)
  )

  def showPage = auth { implicit request =>
    Ok(viewEoriAction(form.fill(EoriAction(EoriActionEnum.UPDATE_EORI.toString))))
  }

  def showPageOnSuccess(
    cancelOrUpdate: Option[String],
    oldEoriNumber: Option[String],
    newEoriNumber: Option[String],
    subscribedEnrolments: Option[String],
    notUpdatableEnrolments: Option[String],
    cancelledEnrolments: Option[String],
    nonCancelableEnrolments: Option[String]
  ) = auth { implicit request =>
    Ok(
      eoriOperationSuccessfulView(
        form.fill(EoriAction(EoriActionEnum.UPDATE_EORI.toString)),
        cancelOrUpdate = cancelOrUpdate,
        oldEoriNumber = oldEoriNumber,
        newEoriNumber = newEoriNumber,
        cancelledEnrolments = cancelledEnrolments,
        notUpdatableEnrolments = notUpdatableEnrolments,
        subscribedEnrolments = subscribedEnrolments,
        nonCancelableEnrolments = nonCancelableEnrolments
      )
    )
  }

  def continueAction = auth { implicit request =>
    form
      .bindFromRequest()
      .fold(
        _ => Redirect(controllers.routes.EoriActionController.showPage),
        {
          case EoriAction(cancelOrUpdate) if EoriActionEnum.withName(cancelOrUpdate) == EoriActionEnum.UPDATE_EORI =>
            Redirect(controllers.routes.UpdateEoriController.showPage)
          case EoriAction(cancelOrUpdate) if EoriActionEnum.withName(cancelOrUpdate) == EoriActionEnum.CANCEL_EORI =>
            Redirect(controllers.routes.CancelEoriController.showPage)
          case _ => Redirect(controllers.routes.EoriActionController.showPage)
        }
      )
  }
}
