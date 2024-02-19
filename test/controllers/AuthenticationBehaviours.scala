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

package controllers

import config.AppConfig
import org.mockito.ArgumentMatchers.{any, eq => ameq}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.BodyParsers
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AuthenticationBehaviours { this: MockitoSugar =>

  val env: Environment = Environment.simple()
  val config: Configuration = Configuration.load(env)
  val mockParser: BodyParsers.Default = mock[BodyParsers.Default]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  private val serviceConfig = new ServicesConfig(config)
  val appConfig = new AppConfig(config, serviceConfig, "TestApp")
  val testAuthAction: AuthAction = new AuthAction(mockAuthConnector, config, appConfig, mockParser)

  def withSignedInUser(test: => Unit): Unit =
    withSignedInUser(someUser)(test)

  private def withSignedInUser[T](user: SignedInUser)(test: => T): T = {
    when(
      mockAuthConnector
        .authorise(any(), ameq(credentials and name and email and internalId and allEnrolments))(any(), any())
    )
      .thenReturn(
        Future.successful(
          new ~(
            new ~(
              new ~(new ~(Some(Credentials(user.pid, "PrivilegedApplication")), Some(user.name)), user.email),
              user.internalId
            ),
            user.enrolments
          )
        )
      )
    test
  }

  def withNotSignedInUser(test: => Unit): Unit = {
    when(mockAuthConnector.authorise(any(), any[Retrieval[_]])(any[HeaderCarrier], any()))
      .thenReturn(Future.failed(new NoActiveSession("A user is not logged in") {}))

    test
  }

  val someUser: SignedInUser = SignedInUser(
    "123456789",
    Name(Some("John"), Some("Doe")),
    "update-enrolment-eori",
    Some("johndoe@example.co.uk"),
    Some("Int-ba17b467-90f3-42b6-9570-73be7b78eb2b"),
    Enrolments(Set(Enrolment("update-enrolment-eori", List(EnrolmentIdentifier("Requestor", "")), "Activated", None)))
  )
}
