/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package views

import play.api.data.Form
import play.api.i18n.Messages

object ViewUtils {
  def title(form: Form[_], titleStr: String, section: Option[String] = None)(implicit messages: Messages): String =
    titleNoForm(s"${errorPrefix(form)} ${messages(titleStr)}", section)

  def titleNoForm(title: String, section: Option[String] = None)(implicit messages: Messages): String =
    s"${messages(title)} - ${section.fold("")(messages(_) + " - ")}${messages("service.name")} - ${messages("site.govuk")}"

  def errorPrefix(form: Form[_])(implicit messages: Messages): String = {
    if (form.hasErrors || form.hasGlobalErrors) messages("error.browser.title.prefix") else ""
  }
}