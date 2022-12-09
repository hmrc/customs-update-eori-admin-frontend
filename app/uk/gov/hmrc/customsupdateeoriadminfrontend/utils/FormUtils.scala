/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.customsupdateeoriadminfrontend.utils

import play.api.data.Forms.nonEmptyText

import java.time.LocalDate
import play.api.data.Mapping
import uk.gov.hmrc.customsupdateeoriadminfrontend.utils.DateTuple._

object FormUtils {

  val messageKeyMandatoryField = "cds.error.mandatory.field"
  val messageKeyInvalidDateFormat = "cds.error.invalid.date.format"
  val messageKeyFutureDate = "cds.error.future-date"
  val messageKeyInvalidEori = "cds.error.invalid.eori"

  private val eoriRegex: String = "^(GB)[0-9]{12,15}$"

  def checkMandatoryDate(onEmptyError: String = messageKeyMandatoryField,
                         onInvalidDateError: String =
                           messageKeyInvalidDateFormat): Mapping[LocalDate] = {
    dateTuple(invalidDateError = onInvalidDateError)
      .verifying(onEmptyError, d => d.isDefined)
      .transform(_.get, Option(_))
  }

  def mandatoryDate(onEmptyError: String = messageKeyMandatoryField,
                    onInvalidDateError: String = messageKeyInvalidDateFormat,
                    onDateInFutureError: String = messageKeyFutureDate)
    : Mapping[LocalDate] = {
    checkMandatoryDate(onEmptyError, onInvalidDateError)
      .verifying(onDateInFutureError, d => {
        val today = LocalDate.now()
        d.isEqual(today) || d.isBefore(today)
      })
  }
  
  def checkEori: Mapping[String] = nonEmptyText.verifying(messageKeyInvalidEori, name => name.isEmpty || name.matches(eoriRegex))
}
