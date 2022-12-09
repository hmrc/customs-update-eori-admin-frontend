/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.customsupdateeoriadminfrontend.connector

import play.api.Configuration

trait ContextBuilder {
  val configuration: Configuration
  lazy val enrolmentServiceBaseContext: String =
    configuration.get[String]("enrolment.service.context")
  lazy val customsDataStoreBaseContext: String =
    configuration.get[String]("customs-data-store.context")
  lazy val customsDataStoreToken: String = "Bearer " + configuration
    .get[String]("customs-data-store.token")
}
