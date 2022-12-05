/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.customsupdateeoriadminfrontend.controllers

import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.customsupdateeoriadminfrontend.model.EoriAction
import uk.gov.hmrc.customsupdateeoriadminfrontend.views.html.eori_action
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
case class EoriActionController @Inject()(mcc: MessagesControllerComponents,
                                          viewEoriAction: eori_action)
  extends FrontendController(mcc) with I18nSupport {

  val form: Form[EoriAction] = Form(mapping(
    "update-or-cancel-eori" -> text()
  )(EoriAction.apply)(EoriAction.unapply))

  def showPage(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(viewEoriAction(form)))
  }

  def continueAction(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok("continue"))
  }
}



