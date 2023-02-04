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

import play.api.data.FormError
import play.api.data.format.Formatter

private[mappings] class EoriNumberFormatter(requiredKey: String,
                                            patternNotMatchingKey: String,
                                            args: Seq[String] = Seq.empty
                                           ) extends Formatter[String] with Formatters {
  private val EORI_NUMBER_PATTERN = "^[G][B][0-9]{12}$"

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
    val stringFormatted = stringFormatter(requiredKey, args).bind(key, data)
    stringFormatted.flatMap {
      case str if str.matches(EORI_NUMBER_PATTERN) => Right(str)
      case _ => Left(List(FormError(key, patternNotMatchingKey, args)))
    }
  }

  override def unbind(key: String, value: String): Map[String, String] = {
    Map(key -> value)
  }
}
