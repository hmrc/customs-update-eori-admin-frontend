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

import models.LocalDateBinder
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.FormError

import java.time.LocalDate

class LocalDateFormattersSpec extends AnyWordSpec with Matchers with OptionValues with Mappings {

  val invalidKey = "error.invalidKey"
  val invalidYear = "eori.validation.establishmentDate.invalidYear"
  val oneDateComponentMissingKey = "error.oneDateComponentMissingKey"
  val twoDateComponentsMissingKey = "error.twoDateComponentMissingKey"
  val threeDateComponentsMissingKey = "error.threeDateComponentsMissingKey"
  val mustBeInPastKey = "error.mustBeInPastKey"
  val field = "field"
  val localDateFormatter = new LocalDateFormatter(
    invalidKey,
    invalidYear,
    oneDateComponentMissingKey,
    twoDateComponentsMissingKey,
    threeDateComponentsMissingKey,
    mustBeInPastKey
  )

  "LocalDateFormatters" should {
    "bind if date is valid" in {
      val result = localDateFormatter.bind(
        field,
        Map(
          s"$field.day"   -> "12",
          s"$field.month" -> "01",
          s"$field.year"  -> "2000"
        )
      )
      result.toOption.get shouldBe LocalDate.parse("12/01/2000", LocalDateBinder.dateTimeFormatter)
    }

    "return oneDateComponentMissingKey error when day is missing with parameter day" in {
      val result = localDateFormatter.bind(
        field,
        Map(
          s"$field.month" -> "01",
          s"$field.year"  -> "2000"
        )
      )
      result.left.toOption.get shouldBe Seq(FormError(field, oneDateComponentMissingKey, List("day")))
    }

    "return oneDateComponentMissingKey error when month is missing with parameter month" in {
      val result = localDateFormatter.bind(
        field,
        Map(
          s"$field.day"  -> "12",
          s"$field.year" -> "2000"
        )
      )
      result.left.toOption.get shouldBe Seq(FormError(field, oneDateComponentMissingKey, List("month")))
    }

    "return oneDateComponentMissingKey error when year is missing with parameter year" in {
      val result = localDateFormatter.bind(
        field,
        Map(
          s"$field.day"   -> "12",
          s"$field.month" -> "02"
        )
      )
      result.left.toOption.get shouldBe Seq(FormError(field, oneDateComponentMissingKey, List("year")))
    }

    "return twoDateComponentsMissingKey error when day and month are missing" in {
      val result = localDateFormatter.bind(
        field,
        Map(
          s"$field.year" -> "2000"
        )
      )
      result.left.toOption.get shouldBe Seq(FormError(field, twoDateComponentsMissingKey, List("day", "month")))
    }

    "return twoDateComponentsMissingKey error when month and year are missing" in {
      val result = localDateFormatter.bind(
        field,
        Map(
          s"$field.day" -> "2000"
        )
      )
      result.left.toOption.get shouldBe Seq(FormError(field, twoDateComponentsMissingKey, List("month", "year")))
    }

    "return threeDateComponentsMissingKey error when day and month are missing" in {
      val result = localDateFormatter.bind(field, Map.empty)
      result.left.toOption.get shouldBe Seq(FormError(field, threeDateComponentsMissingKey))
    }

    "return invalidKey error when date is wrong" in {
      val result = localDateFormatter.bind(
        field,
        Map(
          s"$field.day"   -> "AB",
          s"$field.month" -> "01",
          s"$field.year"  -> "2000"
        )
      )
      result.left.toOption.get shouldBe Seq(FormError(field, invalidKey))
    }

    "return mustBeInPastKey error when date is in future" in {
      val futureDate = LocalDate.now().plusDays(2)
      val result = localDateFormatter.bind(
        field,
        Map(
          s"$field.day"   -> futureDate.getDayOfMonth.toString,
          s"$field.month" -> futureDate.getMonthValue.toString,
          s"$field.year"  -> futureDate.getYear.toString
        )
      )
      result.left.toOption.get shouldBe Seq(FormError(field, mustBeInPastKey))
    }
  }
}
