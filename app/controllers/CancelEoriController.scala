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

package controllers

import audit.Auditable
import config.AppConfig
import mappings.Mappings
import models.EoriEventEnum.CANCEL
import models.ValidateEori.{ESTABLISHMENT_DATE_WRONG, TRUE}
import models._
import models.events.CancelEoriEvent
import play.api.data.Forms._
import play.api.data.{Form, FormError, Forms}
import play.api.i18n.{I18nSupport, Lang}
import play.api.libs.json.Json
import play.api.mvc._
import service.EnrolmentService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.{CancelEoriProblemView, CancelEoriView, ConfirmCancelEoriView}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
case class CancelEoriController @Inject()(mcc: MessagesControllerComponents,
                                          viewCancelEori: CancelEoriView,
                                          viewConfirmCancelEori: ConfirmCancelEoriView,
                                          cancelEoriProblemView: CancelEoriProblemView,
                                          auth: AuthAction,
                                          enrolmentService: EnrolmentService,
                                          config: AppConfig,
                                          audit: Auditable
                                         )(implicit ec: ExecutionContext)
  extends FrontendController(mcc)
    with Mappings
    with I18nSupport {

  private val SPLITTER_CHARACTER = ","

  val formCancelEori = Form(
    mapping(
      "existing-eori" -> eoriNumber(
        "eori.validation.existingEori.required",
        "eori.validation.existingEori.format"
      ),
      "date-of-establishment" -> localDate(
        invalidKey = "eori.validation.establishmentDate.invalid",
        invalidYear = "eori.validation.establishmentDate.invalidYear",
        threeDateComponentsMissingKey = "eori.validation.establishmentDate.required.all",
        twoDateComponentsMissingKey = "eori.validation.establishmentDate.required.two",
        oneDateComponentMissingKey = "eori.validation.establishmentDate.required.one",
        mustBeInPastKey = "eori.validation.establishmentDate.mustBeInPast",
      )
    )(EoriCancel.apply)(EoriCancel.unapply))

  val formConfirmCancelEori = Form(
    mapping(
      "existing-eori" -> text(),
      "date-of-establishment" -> Forms.localDate(LocalDateBinder.dateTimePattern),
      "enrolment-list" -> text(),
      "not-cancellable-enrolment-list" -> text()
    )(ConfirmEoriCancel.apply)(ConfirmEoriCancel.unapply))

  def showPage = auth { implicit request =>
    Ok(viewCancelEori(formCancelEori))
  }

  def continueCancelEori = auth.async { implicit request =>
    formCancelEori.bindFromRequest().fold(
      formWithError => Future(BadRequest(viewCancelEori(formWithError))),
      eoriCancel => enrolmentService.getEnrolments(CANCEL, Eori(eoriCancel.existingEori), eoriCancel.dateOfEstablishment)
        .map(enrolments => {
          if (enrolments.exists(_._2 == ESTABLISHMENT_DATE_WRONG)) {
            BadRequest(viewCancelEori(formCancelEori.fill(eoriCancel).withError(FormError("date-of-establishment", mcc.messagesApi.apply("eori.validation.establishmentDate.mustBeMatched")(Lang("en"))))))
          } else {
            val enrolmentList = enrolments.filter(_._2 == TRUE).map(_._1).toList
            val cancelableEnrolments = enrolmentList.filter(e => CancelableEnrolments.values.contains(e))
            val notCancelableEnrolments = enrolmentList.filter(e => !CancelableEnrolments.values.contains(e))
            Ok(viewConfirmCancelEori(
              formConfirmCancelEori.fill(ConfirmEoriCancel(eoriCancel.existingEori, eoriCancel.dateOfEstablishment, cancelableEnrolments.mkString(SPLITTER_CHARACTER), notCancelableEnrolments.mkString(SPLITTER_CHARACTER))),
              cancelableEnrolments,
              notCancelableEnrolments
            ))
          }
        })
    )
  }

  def confirmCancelEori = auth.async { implicit request =>
    formConfirmCancelEori.bindFromRequest().fold(
      _ => {
        Future(Redirect(controllers.routes.CancelEoriController.showPage))
      },
      confirmEoriCancel => {
        val cancelAllEnrolments = Future.sequence(
          confirmEoriCancel.enrolmentList.split(SPLITTER_CHARACTER)
            .toList
            .map(EnrolmentKey.getEnrolmentKey(_).get)
            .map(enrolment =>
              enrolmentService.cancel(
                Eori(confirmEoriCancel.existingEori),
                confirmEoriCancel.dateOfEstablishment,
                enrolment
              ).map(enrolment.serviceName -> _)
            )
        )
        cancelAllEnrolments.map { updates => {
          val status = updates.map(either => either._1 -> either._2.isRight).toMap
          if (status.exists(_._2 == false)) {
            auditCall(CancelEoriEvent(
              eoriNumber = confirmEoriCancel.existingEori,
              dateOfEstablishment = LocalDateBinder.localDateToString(confirmEoriCancel.dateOfEstablishment),
              status = AuditStatus.FAILED,
              failedServices = status.filter(_._2 == false).keys.toList
            ))
            Ok(cancelEoriProblemView(status.filter(_._2 == true).keys.toList, status.filter(_._2 == false).keys.toList, confirmEoriCancel.existingEori))
          } else {
            auditCall(CancelEoriEvent(
              eoriNumber = confirmEoriCancel.existingEori,
              dateOfEstablishment = LocalDateBinder.localDateToString(confirmEoriCancel.dateOfEstablishment),
              status = AuditStatus.OK,
              cancelledServices = status.filter(_._2 == true).keys.toList
            ))
            Redirect(controllers.routes.EoriActionController.showPageOnSuccess(
              cancelOrUpdate = Some(EoriActionEnum.CANCEL_EORI.toString),
              oldEoriNumber = Some(confirmEoriCancel.existingEori),
              cancelledEnrolments = Some(confirmEoriCancel.enrolmentList),
              nonCancelableEnrolments = Some(confirmEoriCancel.notCancellableEnrolmentList)
            ))
          }
        }
        }
      }
    )
  }

  private def auditCall(details: CancelEoriEvent)(implicit hc: HeaderCarrier): Unit =
    audit.sendExtendedDataEvent(
      transactionName = "CancelEoriNumber",
      path = s"https://${config.appName}.mdtp:443/confirm/confirm-cancel",
      details = Json.toJson(details),
      eventType = "CancelEoriNumber",
    )

}
