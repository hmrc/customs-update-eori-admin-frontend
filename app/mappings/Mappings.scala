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

import play.api.data.FieldMapping
import play.api.data.Forms.of

import java.time.LocalDate

trait Mappings extends Formatters {

  protected def localDate(
    invalidKey: String,
    invalidYear: String,
    oneDateComponentMissingKey: String,
    twoDateComponentsMissingKey: String,
    threeDateComponentsMissingKey: String,
    mustBeInPastKey: String,
    args: Seq[String] = Seq.empty
  ): FieldMapping[LocalDate] =
    of(
      new LocalDateFormatter(
        invalidKey,
        invalidYear,
        oneDateComponentMissingKey,
        twoDateComponentsMissingKey,
        threeDateComponentsMissingKey,
        mustBeInPastKey,
        args
      )
    )

  protected def eoriNumber(
    requiredKey: String,
    patternNotMatchingKey: String,
    args: Seq[String] = Seq.empty
  ): FieldMapping[String] =
    of(
      new EoriNumberFormatter(
        requiredKey,
        patternNotMatchingKey,
        args
      )
    )
}
