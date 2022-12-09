/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.customsupdateeoriadminfrontend.controllers

import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.customsupdateeoriadminfrontend.models.CancelEnrolments
import uk.gov.hmrc.customsupdateeoriadminfrontend.models.CancelSubscription

import uk.gov.hmrc.customsupdateeoriadminfrontend.views.html.cancel_subscriptions
import uk.gov.hmrc.customsupdateeoriadminfrontend.views.html.cancel_enrolments

import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
case class CancelSubscriptionsController @Inject()(mcc: MessagesControllerComponents,
                                                   viewCancelSubscriptions: cancel_subscriptions)
  extends FrontendController(mcc) with I18nSupport {

  val form: Form[CancelSubscription] = Form(mapping(
    "current-eori" -> text(),
    "estdate" -> text()
  )(CancelSubscription.apply)(CancelSubscription.unapply))

  def showPage(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(viewCancelSubscriptions(form)))
  }

  def confirmCancelSubscription(): Action[AnyContent] = Action { implicit request =>
     val formContent = request.body.asFormUrlEncoded
      Future.successful(Ok("####In cancel subscription"))
      Redirect(uk.gov.hmrc.customsupdateeoriadminfrontend.controllers.routes.EoriActionController.showPage())

  }
}
