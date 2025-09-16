/*
 * Copyright 2025 HM Revenue & Customs
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

package mappings

import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.FormError

class EoriNumberFormattersSpec extends AnyWordSpec with Matchers with OptionValues with Mappings {

  val requiredKey = "error.required"
  val patternNotMatchingKey = "error.patternNotMatching"
  val field = "field"
  val eoriNumberFormatter = new EoriNumberFormatter(requiredKey, patternNotMatchingKey)

  "EoriNumberFormatter" should {
    "bind if EORI Number is valid" in {
      val result = eoriNumberFormatter.bind(field, Map(field -> "GB123456789012"))
      result.toOption.get shouldBe "GB123456789012"
    }

    "return requiredKey error when EORI number string is empty" in {
      val result = eoriNumberFormatter.bind(field, Map(field -> ""))
      result.left.toOption.get shouldBe (Seq(FormError(field, requiredKey)))
    }

    "return patternNotMatchingKey error when EORI number doesn't start with GB" in {
      val result = eoriNumberFormatter.bind(field, Map(field -> "AB123456789012"))
      result.left.toOption.get shouldBe (Seq(FormError(field, patternNotMatchingKey)))
    }

    "return patternNotMatchingKey error when EORI number doesn't have 12 digits after GB" in {
      val result = eoriNumberFormatter.bind(field, Map(field -> "GB1234567"))
      result.left.toOption.get shouldBe (Seq(FormError(field, patternNotMatchingKey)))
    }
  }
}
