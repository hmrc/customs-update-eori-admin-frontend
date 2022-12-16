/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.auth.core.retrieve.Name
import models.{EoriUpdate}
import views.html.UpdateEoriView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{Future,ExecutionContext}
import scala.concurrent.Future

class UpdateEoriController @Inject()(
    mcc: MessagesControllerComponents,
    viewUpdateEori: UpdateEoriView)(implicit ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport {

  val form = Form(
    mapping(
      "existing-eori" -> text(),
      "date-of-establishment" -> text(),
      "new-eori" -> text()
    )(EoriUpdate.apply)(EoriUpdate.unapply))

  def showPage = Action { implicit request =>
    Ok(viewUpdateEori(form))
  }

  /*def continueUpdateEori() = Action { implicit request =>
    Ok(uk.gov.hmrc.customsupdateeoriadminfrontend.controllers.routes.html.ConfirmEoriUpdateView)
  }*/

}
