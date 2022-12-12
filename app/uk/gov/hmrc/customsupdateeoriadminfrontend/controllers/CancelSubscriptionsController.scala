/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.customsupdateeoriadminfrontend.controllers

import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.customsupdateeoriadminfrontend.models.CancelSubscription
import uk.gov.hmrc.customsupdateeoriadminfrontend.views.html.cancel_subscriptions
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
case class CancelSubscriptionsController @Inject()(mcc: MessagesControllerComponents,
                                                   viewCancelSubscriptions: cancel_subscriptions)
  extends FrontendController(mcc) with I18nSupport {

  val form: Form[CancelSubscription] = Form(mapping(
    "confirm-cancel" -> text()
  )(CancelSubscription.apply)(CancelSubscription.unapply))

  def showPage(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(viewCancelSubscriptions(form)))
  }

  def confirmCancelSubscription() = Action { implicit request =>
    val formContent = request.body.asFormUrlEncoded
    formContent.map { args =>
      val actionSelected = args("confirm-cancel").head

      if (actionSelected != null && actionSelected == "Yes") {
        // Add logic for respective services to cancel subscription

        // On successful cancel redirect to main page
        Redirect(uk.gov.hmrc.customsupdateeoriadminfrontend.controllers.routes.EoriActionController.showPage())
      }
      else if (actionSelected != null && actionSelected == "No") {
        Redirect(uk.gov.hmrc.customsupdateeoriadminfrontend.controllers.routes.CancelEnrolmentsController.showPage())
      }
      else Redirect(uk.gov.hmrc.customsupdateeoriadminfrontend.controllers.routes.CancelEnrolmentsController.showPage())
    }.getOrElse(
      // Future.successful(Ok("####In confirmCancelSubscription ")))
      Redirect(uk.gov.hmrc.customsupdateeoriadminfrontend.controllers.routes.EoriActionController.showPage()))

  }
}
