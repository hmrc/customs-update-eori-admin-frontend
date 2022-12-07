/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.customsupdateeoriadminfrontend.controllers

import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.customsupdateeoriadminfrontend.models.EoriAction
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

  def showPage() = Action.async { implicit request =>
    Future.successful(Ok(viewEoriAction(form)))
  }

  def continueAction() = Action { implicit request =>
    val formContent = request.body.asFormUrlEncoded
    println(s"###### action is: $formContent")
    formContent.map { args =>
      val actionSelected = args("update-or-cancel-eori").head
      println(s"#####Action selected $actionSelected")
      if (actionSelected == "updateeori") Redirect(uk.gov.hmrc.customsupdateeoriadminfrontend.controllers.routes.EoriActionController.showPage())
      else if (actionSelected == "canceleori") Redirect(uk.gov.hmrc.customsupdateeoriadminfrontend.controllers.routes.CancelSubscriptionController.showPage())
      else Redirect(uk.gov.hmrc.customsupdateeoriadminfrontend.controllers.routes.EoriActionController.showPage())

      //Future.successful(Ok(s"####continue $actionSelected"))
      //Redirect(uk.gov.hmrc.customsupdateeoriadminfrontend.controllers.routes.EoriActionController.showPage())
    }.getOrElse(
      Redirect(uk.gov.hmrc.customsupdateeoriadminfrontend.controllers.routes.EoriActionController.showPage()))

  }
}



