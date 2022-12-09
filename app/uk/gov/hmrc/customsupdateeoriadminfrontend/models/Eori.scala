/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.customsupdateeoriadminfrontend.models


import play.api.libs.json.Json

case class Eori(eori: String) {
  override def toString: String = eori
}

object Eori {
  implicit val formats = Json.format[Eori]
}

