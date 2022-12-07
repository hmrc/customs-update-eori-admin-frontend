/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.customsupdateeoriadminfrontend.models

import java.time.LocalDate

case class EoriUpdate(existingEori: String, date: LocalDate, newEori: String)
