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

package models



object EnrolmentKey extends Enumeration {
  type EnrolmentKeyType = EnrolmentKeyVal
  case class EnrolmentKeyVal(serviceName: String, description: String) extends super.Val {
    def getEnrolmentKey(eori: Eori): String = s"$serviceName~EORINumber~$eori"
    def getEnrolmentKey(eori: String): String = s"$serviceName~EORINumber~$eori"
  }

  import scala.language.implicitConversions
  implicit def valueToEnrolmentKeyVal(x: Value): EnrolmentKeyVal = x.asInstanceOf[EnrolmentKeyVal]

  // HMRC-CUS-ORG*  Customs Declaration Service (Existing Service from Old customs update eori)
  val HMRC_CUS_ORG = EnrolmentKeyVal("HMRC-CUS-ORG", "Customs Declaration Service")

  // HMRC-ATAR-ORG*  Advance Tariff Registration
  val HMRC_ATAR_ORG = EnrolmentKeyVal("HMRC-ATAR-ORG", "Advance Tariff Registration")

  // HMRC-GVMS-ORG*  Goods Vehicle Movement System
  val HMRC_GVMS_ORG = EnrolmentKeyVal("HMRC-GVMS-ORG", "Goods Vehicle Movement System")

  // HMRC-SS-ORG*	 Safety & Security Great Britain
  val HMRC_SS_ORG = EnrolmentKeyVal("HMRC-SS-ORG", "GB Safety & Security")

  // HMRC-CTS-ORG*  Customs Trader Services (Services: Route 1, NDRC, C18)
  val HMRC_CTS_ORG = EnrolmentKeyVal("HMRC-SS-ORG", "Customs Trader Services")
}