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

package models

import models.EnrolmentKey._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EnrolmentKeySpec
  extends AnyWordSpec
  with Matchers {

  "EnrolmentKey getDescription" should {
    "return the correct description for valid keys" in {
      EnrolmentKey.getDescription(HMRC_CUS_ORG.serviceName).get shouldBe HMRC_CUS_ORG.description
      EnrolmentKey.getDescription(HMRC_ATAR_ORG.serviceName).get shouldBe HMRC_ATAR_ORG.description
      EnrolmentKey.getDescription(HMRC_GVMS_ORG.serviceName).get shouldBe HMRC_GVMS_ORG.description
      EnrolmentKey.getDescription(HMRC_SS_ORG.serviceName).get shouldBe HMRC_SS_ORG.description
      EnrolmentKey.getDescription(HMRC_CTS_ORG.serviceName).get shouldBe HMRC_CTS_ORG.description
      EnrolmentKey.getDescription(HMRC_ESC_ORG.serviceName).get shouldBe HMRC_ESC_ORG.description
    }

    "return the None if key is not valid" in {
      EnrolmentKey.getDescription("DUMMY") shouldBe None
    }
  }

  "EnrolmentKey getEnrolmentKey" should {
    "return the correct description for valid keys" in {
      EnrolmentKey.getEnrolmentKey(HMRC_CUS_ORG.serviceName).get shouldBe HMRC_CUS_ORG
      EnrolmentKey.getEnrolmentKey(HMRC_ATAR_ORG.serviceName).get shouldBe HMRC_ATAR_ORG
      EnrolmentKey.getEnrolmentKey(HMRC_GVMS_ORG.serviceName).get shouldBe HMRC_GVMS_ORG
      EnrolmentKey.getEnrolmentKey(HMRC_SS_ORG.serviceName).get shouldBe HMRC_SS_ORG
      EnrolmentKey.getEnrolmentKey(HMRC_CTS_ORG.serviceName).get shouldBe HMRC_CTS_ORG
      EnrolmentKey.getEnrolmentKey(HMRC_ESC_ORG.serviceName).get shouldBe HMRC_ESC_ORG
    }

    "return the None if key is not valid" in {
      EnrolmentKey.getEnrolmentKey("DUMMY") shouldBe None
    }
  }
}
