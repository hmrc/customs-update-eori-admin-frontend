/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.customsupdateeoriadminfrontend.models


case class EnrolmentKey(key: String) {
  override def toString: String = key
}

object EnrolmentKey {
  def apply(eori: Eori): EnrolmentKey =
    EnrolmentKey(s"HMRC-CUS-ORG~EORINumber~$eori")
}

