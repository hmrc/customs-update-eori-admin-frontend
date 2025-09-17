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

package service.testOnly

import config.AppConfig
import models.LocalDateBinder.localDateToString
import models.testOnly._
import play.api.http.Status.{CREATED, NO_CONTENT}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StubDataService @Inject() (httpClient: HttpClient, config: AppConfig)(implicit ec: ExecutionContext) {
  def createEoriNumber(createEori: CreateEori)(implicit hc: HeaderCarrier): Future[Seq[Int]] = {
    val seq = createKnownFacts(createEori).toSeq :+ createGroupPersona(createEori)
    Future.sequence(seq)
  }

  private def createKnownFacts(createEori: CreateEori)(implicit hc: HeaderCarrier) = {
    val enrolments = createEori.enrolments.split(",").map(_.replaceAll(" ", ""))
    enrolments.map { enrolment =>
      val knownFactPersona = KnownFactPersona(
        enrolment,
        Seq(
          KnownFact("EORINumber", createEori.eoriNumber, "identifier"),
          KnownFact("DateOfEstablishment", localDateToString(createEori.dateOfEstablishment), "verifier")
        )
      )

      httpClient
        .POST[KnownFactPersona, HttpResponse](
          s"${config.enrolmentStoreProxyBaseServiceUrl}/enrolment-store-stub/known-facts",
          knownFactPersona,
          Seq(("content-type", "application/json"))
        )
        .flatMap { result =>
          if (result.status == CREATED)
            Future.successful(result.status)
          else
            Future.failed(new Exception("Failed"))
        }
    }
  }

  private def createGroupPersona(createEori: CreateEori)(implicit hc: HeaderCarrier) = {
    val enrolments = createEori.enrolments.split(",").map(_.replaceAll(" ", ""))
    val seqOfEnrolments = enrolments.map { enrolment =>
      Enrolment(
        serviceName = enrolment,
        identifiers = Seq(Identifier("EORINumber", createEori.eoriNumber))
      )
    }.toSeq

    val groupPersona = GroupPersona(
      groupId = UUID.randomUUID().toString,
      users = Seq(
        User(
          credId = UUID.randomUUID().toString
        )
      ),
      enrolments = seqOfEnrolments
    )
    httpClient
      .POST[GroupPersona, HttpResponse](
        s"${config.enrolmentStoreProxyBaseServiceUrl}/enrolment-store-stub/data",
        groupPersona,
        Seq(("content-type", "application/json"))
      )
      .flatMap { result =>
        if (result.status == NO_CONTENT)
          Future.successful(result.status)
        else
          Future.failed(new Exception("Failed"))
      }
  }
}
