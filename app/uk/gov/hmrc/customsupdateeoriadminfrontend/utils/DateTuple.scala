/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.customsupdateeoriadminfrontend.utils

import java.time.LocalDate

import play.api.data.Forms.{optional, text, tuple}
import play.api.data.Mapping
import uk.gov.hmrc.play._ //mappers.DateFields._

import scala.util.Try

object DateTuple {

  def dateTuple(validate: Boolean = true,
                invalidDateError: String = "cds.error.invalid.date.format")
    : Mapping[Option[LocalDate]] = {
    def tuple2Date(tuple: (Option[String], Option[String], Option[String])) =
      tuple match {
        case (Some(y), Some(m), Some(d)) =>
          try {
            Some(LocalDate.of(y.trim.toInt, m.trim.toInt, d.trim.toInt))
          } catch {
            case e: Exception if validate => throw e
            case _: Throwable             => None
          }

        case _ => None
      }

    def date2Tuple(maybeDate: Option[LocalDate]) = maybeDate match {
      case Some(d) =>
        (Some(d.getYear.toString),
         Some(d.getMonthValue.toString),
         Some(d.getDayOfMonth.toString))
      case _ => (None, None, None)
    }

    dateTupleMapping
      .verifying(
        invalidDateError,
        _ match {
          case (None, None, None) => true

          case (yearOption, monthOption, dayOption) if validate =>
            Try({
              val y = yearOption
                .getOrElse(throw new Exception("Year missing"))
                .trim
                .toInt
              if (!(1000 to 9999 contains y))
                throw new Exception("Year must be 4 digits")

              val m =
                monthOption.getOrElse(throw new Exception("Month missing"))
              val d = dayOption.getOrElse(throw new Exception("Day missing"))
              LocalDate.of(y, m.trim.toInt, d.trim.toInt)
            }).isSuccess

          case _ => true
        }
      )
      .transform[Option[LocalDate]](tuple2Date, date2Tuple)
  }

  private val dateTupleMapping =
    tuple(
      "year" -> optional(text),
      "month" -> optional(text),
      "day" -> optional(text)
    )

}
