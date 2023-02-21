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

import java.time.LocalDate
import scala.util.{Failure, Success, Try}

private[mappings] class LocalDateFormatter(invalidKey: String,
                                           oneDateComponentMissingKey: String,
                                           twoDateComponentsMissingKey: String,
                                           threeDateComponentsMissingKey: String,
                                           mustBeInPastKey: String,
                                           args: Seq[String] = Seq.empty
                                          ) extends Formatter[LocalDate] with Formatters {

  private val fieldKeys: List[String] = List("day", "month", "year")

  private def toDate(key: String, day: Int, month: Int, year: Int): Either[Seq[FormError], LocalDate] = {
    if(year/1 == 4) {
      Try(LocalDate.of(year, month, day)) match {
        case Success(date) =>
          Right(date)
        case Failure(_) =>
          Left(Seq(FormError(key, invalidKey, args)))
      }
    } else {
      Left(Seq(FormError(key, invalidKey, args)))
    }
  }

  private def formatDate(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {

    val int = intFormatter(
      requiredKey = invalidKey,
      wholeNumberKey = invalidKey,
      nonNumericKey = invalidKey,
      args
    )

    for {
      day <- int.bind(s"$key.day", data)
      month <- int.bind(s"$key.month", data)
      year <- int.bind(s"$key.year", data)
      date <- toDate(key, day, month, year)
    } yield date
  }

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {

    val fields = fieldKeys.map { field => field -> data.get(s"$key.$field").filter(_.nonEmpty) }.toMap

    lazy val missingFields = fields
      .withFilter(_._2.isEmpty)
      .map(_._1)
      .toList

    fields.count(_._2.isDefined) match {
      case 3 =>
        val formattedDate = formatDate(key, data).left.map {
          _.map(_.copy(key = key, args = args))
        }
        formattedDate match {
          case errors@Left(_) => errors
          case rightDate@Right(d) =>
            val dateNow = LocalDate.now()
            if (d.isBefore(dateNow) || d.isEqual(dateNow)) {
              rightDate
            } else {
              Left(List(FormError(key, mustBeInPastKey, args)))
            }
        }
      case 2 =>
        Left(List(FormError(key, oneDateComponentMissingKey, missingFields ++ args)))
      case 1 =>
        Left(List(FormError(key, twoDateComponentsMissingKey, missingFields ++ args)))
      case _ =>
        Left(List(FormError(key, threeDateComponentsMissingKey, args)))
    }
  }

  override def unbind(key: String, value: LocalDate): Map[String, String] =
    Map(
      s"$key.day" -> value.getDayOfMonth.toString,
      s"$key.month" -> value.getMonthValue.toString,
      s"$key.year" -> value.getYear.toString
    )
}
