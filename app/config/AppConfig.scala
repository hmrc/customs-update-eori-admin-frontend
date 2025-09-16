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

package config

import javax.inject.{Inject, Named, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject() (
  config: Configuration,
  servicesConfig: ServicesConfig,
  @Named("appName") val appName: String
) {
  lazy val isShuttered: Boolean = config.get[Boolean]("shuttered")
  val enrolmentStoreProxyBaseServiceUrl: String = servicesConfig.baseUrl("enrolment-store-proxy")
  val enrolmentStoreProxyServiceUrl: String =
    s"${servicesConfig.baseUrl("enrolment-store-proxy")}/enrolment-store-proxy"
  val taxEnrolmentsServiceUrl: String = s"${servicesConfig.baseUrl("tax-enrolments")}/tax-enrolments"
  val customsDataStoreUrl: String =
    s"${servicesConfig.baseUrl("customs-data-store")}/customs-data-store/update-eori-history"

  lazy val strideLoginUrl: String = servicesConfig.getString("stride-auth-frontend.sign-in")
  lazy val defaultOrigin: String = config
    .getOptional[String]("sosOrigin")
    .orElse(config.getOptional[String]("appName"))
    .getOrElse("undefined")

  val timeoutSeconds: Int   = config.get[Int]("session.timeoutSeconds")
  val countdownSeconds: Int = config.get[Int]("session.countdownSeconds")
}
