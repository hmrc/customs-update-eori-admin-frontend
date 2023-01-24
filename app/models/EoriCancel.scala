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

case class EoriCancel(existingEori: String, dateOfEstablishment: String)

object EoriCancel {
  def apply(existingEori: String,
            dateOfEstablishmentDay: String,
            dateOfEstablishmentMonth: String,
            dateOfEstablishmentYear: String): EoriCancel =
    new EoriCancel(
      existingEori,
      s"$dateOfEstablishmentDay/$dateOfEstablishmentMonth/$dateOfEstablishmentYear"
    )

  def unapply(eoriCancel: EoriCancel): Option[(String, String, String, String)] = {
    //simple argument extractor
    val parts = eoriCancel.dateOfEstablishment.split("/")
    if (parts.length == 3) Some(eoriCancel.existingEori, parts(0), parts(1), parts(2)) else None
  }
}
