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

package controllers

import com.google.inject.Inject
import config.AppConfig
import play.api.mvc.Results._
import play.api.mvc._
import play.api.Configuration
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{Name, ~}
import uk.gov.hmrc.auth.core.{Enrolments, _}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthAction @Inject() (
  override val authConnector: AuthConnector,
  config: Configuration,
  appConfig: AppConfig,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends ActionBuilder[AuthenticatedRequest, AnyContent] with ActionRefiner[Request, AuthenticatedRequest]
    with AuthorisedFunctions {
  private val isLocal: Boolean = config.get[Boolean]("microservice.services.stride-auth-local")

  private def toStrideLogin(successUrl: String, failureUrl: Option[String] = None): Result =
    Redirect(
      appConfig.strideLoginUrl,
      Map(
        "successURL" -> Seq(successUrl),
        "origin"     -> Seq(appConfig.defaultOrigin)
      ) ++ failureUrl.map(f => Map("failureURL" -> Seq(f))).getOrElse(Map())
    )

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthenticatedRequest[A]]] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val retrievals = credentials and name and email and internalId and allEnrolments
    val roleAllowed = config.get[String]("role.allowed")

    (authorised(AuthProviders(PrivilegedApplication)) retrieve retrievals) {
      case Some(credentials) ~ Some(name) ~ email ~ internalId ~ allEnrolments =>
        val role = allEnrolments.enrolments.find(_.key == roleAllowed) match {
          case Some(r) => r
          case _       => throw InsufficientEnrolments(s"$credentials, $name, $email, $allEnrolments")
        }
        val user = SignedInUser(credentials.providerId, name, role.key, email, internalId, allEnrolments)
        val authenticatedRequest = AuthenticatedRequest(request, user)
        Future.successful(Right(authenticatedRequest))
      case _ => throw InsufficientEnrolments("InsufficientEnrolments")
    }
  } recover {
    case _: NoActiveSession =>
      Left(toStrideLogin {
        if (isLocal) {
          s"http://${request.host}${request.uri}"
        } else {
          s"${request.uri}"
        }
      })
    case _: InsufficientEnrolments =>
      Left(Ok("You are not authorised."))
  }
}

case class AuthenticatedRequest[A](request: Request[A], user: SignedInUser) extends WrappedRequest[A](request)

case class SignedInUser(
  pid: String,
  name: Name,
  role: String,
  email: Option[String],
  internalId: Option[String],
  enrolments: Enrolments
)
