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

package mappings

import org.scalatest.OptionValues
import play.api.data.FormError
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class FormattersSpec extends AnyWordSpec with Matchers with OptionValues with Mappings {

  val requiredKey = "error.required"
  val wholeNumberKey = "error.wholeNumber"
  val nonNumericKey = "error.numericKey"
  val field = "field"

  "stringFormatter" should {
    "bind a valid string" in {
        val result = stringFormatter(requiredKey).bind(field, Map(field -> "TestString"))
        result.toOption.get shouldBe "TestString"
    }

    "return requiredKey error when string empty" in {
      val result = stringFormatter(requiredKey).bind(field, Map(field -> ""))
      result.left.toOption.get shouldBe (Seq(FormError(field, requiredKey)))
    }
  }

  "intFormatter" should {
    "bind a valid number" in {
      val result = intFormatter(requiredKey, wholeNumberKey, nonNumericKey).bind(field, Map(field -> "100"))
      result.toOption.get shouldBe 100
    }

    "return requiredKey error when number string empty" in {
      val result = intFormatter(requiredKey, wholeNumberKey, nonNumericKey).bind(field, Map(field -> ""))
      result.left.toOption.get shouldBe (Seq(FormError(field, requiredKey)))
    }

    "return nonNumericKey error when string empty" in {
      val result = intFormatter(requiredKey, wholeNumberKey, nonNumericKey).bind(field, Map(field -> "ab"))
      result.left.toOption.get shouldBe (Seq(FormError(field, nonNumericKey)))
    }

    "return wholeNumberKey error when string empty" in {
      val result = intFormatter(requiredKey, wholeNumberKey, nonNumericKey).bind(field, Map(field -> "ab12312"))
      result.left.toOption.get shouldBe (Seq(FormError(field, nonNumericKey)))
    }
  }
}
