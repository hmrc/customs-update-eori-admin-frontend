/*
 * Copyright 2022 HM Revenue & Customs
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

package service

import connector._
import models._
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class EnrolmentServiceSpec extends AnyWordSpec with Matchers with ScalaFutures with MockitoSugar with BeforeAndAfterEach {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val mockQueryGroups = mock[QueryGroupsConnector]
  private val mockQueryUsers = mock[QueryUsersConnector]
  private val mockQueryKnownFacts = mock[QueryKnownFactsConnector]

  private val service = new EnrolmentService(mockQueryGroups, mockQueryUsers, mockQueryKnownFacts)

  override def beforeEach(): Unit = {
    reset(mockQueryGroups, mockQueryUsers, mockQueryKnownFacts)
  }

  "Update Eori" should {

    "get enrolments if existing EORI is found by the enrolment service" in {

      val mockData:Enrolment = Enrolment(Seq(KeyValue("EORINumber", "GB123456789000")), Seq(KeyValue("DateOfEstablishment", "03/11/1997")));
      when(mockQueryKnownFacts.queryKnownFacts(meq(Eori("GB123456789000")), meq(LocalDate.of(1997, 11, 3)))(any[HeaderCarrier])).thenReturn(Future.successful(Right(mockData)))
      when(mockQueryUsers.queryUsers(meq(Eori("GB123456789000")))(any[HeaderCarrier])).thenReturn(Future.successful(Right(UserId("0012236665"))))
      when(mockQueryGroups.queryGroups(meq(Eori("GB123456789000")))(any[HeaderCarrier])).thenReturn(Future.successful(Right(GroupId("90ccf333-65d2-4bf2-a008-abc23783"))))

      val result = service.update(Eori("GB123456789000"), LocalDate.of(1997, 11, 3), Eori("GB9866255332")).futureValue
      result shouldBe Right(mockData);
    }

  }
}
