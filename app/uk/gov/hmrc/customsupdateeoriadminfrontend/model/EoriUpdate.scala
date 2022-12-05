/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.customsupdateeoriadminfrontend.model

import java.time.LocalDate

case class EoriUpdate(existingEori: String, date: String, newEori: String)
