/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package controllers

import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import models.EoriAction
import views.html.EoriActionView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
case class EoriActionController @Inject()(mcc: MessagesControllerComponents,
                                          viewEoriAction: EoriActionView)
  extends FrontendController(mcc) with I18nSupport {

  val form: Form[EoriAction] = Form(mapping(
    "update-or-cancel-eori" -> text()
  )(EoriAction.apply)(EoriAction.unapply))

  def showPage = Action.async { implicit request =>
    Future.successful(Ok(viewEoriAction(form)))
  }

  /*
  def continueAction() = Action { implicit request =>
    val formContent = request.body.asFormUrlEncoded
    Future.successful(Ok(viewEoriAction(form)))

    formContent.map { args =>
      val actionSelected = args("update-or-cancel-eori").head
       if (actionSelected != null && actionSelected == "updateeori")  {
        Redirect(controllers.routes.UpdateEoriController.showPage())
      }
      else if (actionSelected != null && actionSelected == "canceleori"){
        Redirect(uk.gov.hmrc.customsupdateeoriadminfrontend.controllers.routes.CancelEnrolmentsController.showPage())
      }
      else Redirect(uk.gov.hmrc.customsupdateeoriadminfrontend.controllers.routes.EoriActionController.showPage())
    }.getOrElse(
      Redirect(uk.gov.hmrc.customsupdateeoriadminfrontend.controllers.routes.EoriActionController.showPage()))

  } */
}



