/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.customsupdateeoriadminfrontend.controllers

import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.customsupdateeoriadminfrontend.audit.Auditor
import uk.gov.hmrc.customsupdateeoriadminfrontend.models.{EoriUpdate}
import uk.gov.hmrc.customsupdateeoriadminfrontend.utils.FormUtils.{checkEori, mandatoryDate}
import uk.gov.hmrc.customsupdateeoriadminfrontend.views.html.update_eori
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext}

class UpdateEoriController @Inject()(
    mcc: MessagesControllerComponents,
    viewUpdateEori: update_eori)(implicit ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport {

  val form = Form(
    mapping(
      "existing-eori" -> checkEori,
      "date-of-establishment" -> mandatoryDate(
        onEmptyError = "cds.error.mandatory.field",
        onInvalidDateError = "cds.error.invalid.date.format"),
      "new-eori" -> checkEori
    )(EoriUpdate.apply)(EoriUpdate.unapply))

  def showPage() = Action { implicit request =>
    Ok(viewUpdateEori(form))
  }


}
