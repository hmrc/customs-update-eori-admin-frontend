/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.customsupdateeoriadminfrontend.controllers

import com.sun.xml.internal.bind.v2.TODO
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.mvc.ControllerHelpers.TODO
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.customsupdateeoriadminfrontend.model.EoriAction
import uk.gov.hmrc.customsupdateeoriadminfrontend.views.html.eori_action
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

case class EoriActionController @Inject()(
     mcc: MessagesControllerComponents,
     viewEoriAction: eori_action)(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
    with I18nSupport {

  val form = Form(
    mapping(
      "update-or-cancel-eori" -> text
    )(EoriAction.apply)(EoriAction.unapply))

  def showPage = Action { implicit request =>
   Ok(viewEoriAction(form))
    //Ok("In show page of eori action")
  }

  def continueAction = Action { implicit request =>
    Ok("In continue")
   }
}



