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

package controllers.testOnly

import mappings.Mappings
import models.testOnly.CreateEori
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import service.testOnly.StubDataService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.testOnly.CreateEoriView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
case class StubDataController @Inject()(
                                         mcc: MessagesControllerComponents,
                                         createEoriView: CreateEoriView,
                                         stubDataService: StubDataService
                                       )(implicit ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with Mappings {

  val form: Form[CreateEori] = Form(
    mapping(
      "eori-number" -> eoriNumber(
        "testOnly.eori.validation.required",
        "testOnly.eori.validation.format"
      ),
      "date-of-establishment" -> localDate(
        invalidKey = "testOnly.establishmentDate.validation.invalid",
        invalidYear = "testOnly.establishmentDate.validation.invalidYear",
        threeDateComponentsMissingKey = "testOnly.establishmentDate.validation.required.all",
        twoDateComponentsMissingKey = "testOnly.establishmentDate.validation.required.two",
        oneDateComponentMissingKey = "testOnly.establishmentDate.validation.required.one",
        mustBeInPastKey = "testOnly.establishmentDate.validation.mustBeInPast"
      ),
      "enrolments" -> nonEmptyText
    )(CreateEori.apply)(CreateEori.unapply)
  )

  def showPage: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(createEoriView(form)))
  }

  def submitCreateRequest: Action[AnyContent] = Action.async { implicit request =>
    form.bindFromRequest()
      .fold(
        formWithError => Future(BadRequest(createEoriView(formWithError))),
        createEori => {
          stubDataService.createEoriNumber(createEori).map { _ =>
            Ok(createEoriView(form, Some(true)))
          }.recoverWith {
            case _ => Future(BadRequest(createEoriView(form, Some(false))))
          }
        }
      )
  }

}
