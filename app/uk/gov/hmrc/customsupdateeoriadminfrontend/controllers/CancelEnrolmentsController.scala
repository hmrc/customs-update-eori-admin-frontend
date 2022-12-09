/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.customsupdateeoriadminfrontend.controllers

import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText, text}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.customsupdateeoriadminfrontend.models.CancelEnrolments
import uk.gov.hmrc.customsupdateeoriadminfrontend.views.html.cancel_enrolments
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
case class CancelEnrolmentsController @Inject()(mcc: MessagesControllerComponents,
                                                  viewCancelEnrolments: cancel_enrolments)
  extends FrontendController(mcc) with I18nSupport {

  val form: Form[CancelEnrolments] = Form(mapping(
    "current-eori-number" -> nonEmptyText(minLength = 0, maxLength = 10),
    "date-of-establishment-day" -> text()
  )(CancelEnrolments.apply)(CancelEnrolments.unapply))

  def showPage(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(viewCancelEnrolments(form)))
  }

  def continueCancelEnrolments(): Action[AnyContent] = Action.async { implicit request =>
    val formContent = request.body.asFormUrlEncoded
    Future.successful(Ok("####In cancel enrolment"))
    //Redirect(uk.gov.hmrc.customsupdateeoriadminfrontend.controllers.routes.EoriActionController.showPage())

  }
}
