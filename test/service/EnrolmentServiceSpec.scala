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

package service

import connector._
import models.EnrolmentKey._
import models.EoriEventEnum.{CANCEL, UPDATE}
import models.ValidateEori.{ESTABLISHMENT_DATE_WRONG, TRUE}
import models._
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{NOT_FOUND, NO_CONTENT}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EnrolmentServiceSpec
    extends AnyWordSpec with Matchers with ScalaFutures with MockitoSugar with BeforeAndAfterEach {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val mockQueryGroups = mock[QueryGroupsConnector]
  private val mockQueryUsers = mock[QueryUsersConnector]
  private val mockQueryKnownFacts = mock[QueryKnownFactsConnector]
  private val mockUpsertKnownFacts = mock[UpsertKnownFactsConnector]
  private val mockDeAllocateGroup = mock[DeAllocateGroupConnector]
  private val mockReAllocateGroup = mock[ReAllocateGroupConnector]
  private val mockRemoveKnownFacts = mock[RemoveKnownFactsConnector]
  private val mockCustomsDataStore = mock[CustomsDataStoreConnector]

  private val service = new EnrolmentService(
    mockQueryGroups,
    mockQueryUsers,
    mockQueryKnownFacts,
    mockUpsertKnownFacts,
    mockDeAllocateGroup,
    mockReAllocateGroup,
    mockRemoveKnownFacts,
    mockCustomsDataStore
  )

  override def beforeEach(): Unit =
    reset(
      mockQueryGroups,
      mockQueryUsers,
      mockQueryKnownFacts,
      mockUpsertKnownFacts,
      mockDeAllocateGroup,
      mockReAllocateGroup,
      mockRemoveKnownFacts,
      mockCustomsDataStore
    )

  val mockGroupIdData: GroupId = GroupId("11111111-1111-1111-1111-111111111111")
  val mockUserIdData: UserId = UserId("1111111111111111")

  "getEnrolments" should {
    "return all services if all exist " in {

      val oldEori = Eori("GB123456789000")
      val dateOfEstablishment = "03/11/1997"

      val mockData: Enrolment = Enrolment(
        Seq(KeyValue("EORINumber", oldEori.toString)),
        Seq(KeyValue("DateOfEstablishment", dateOfEstablishment))
      )

      when(mockQueryGroups.query(meq(oldEori), any[EnrolmentKeyType])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(mockGroupIdData)))

      when(mockQueryUsers.query(meq(oldEori), any[EnrolmentKeyType])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(mockUserIdData)))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CUS_ORG), meq(LocalDate.of(1997, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_ATAR_ORG), meq(LocalDate.of(1997, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_SS_ORG), meq(LocalDate.of(1997, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_GVMS_ORG), meq(LocalDate.of(1997, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CTS_ORG), meq(LocalDate.of(1997, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_ESC_ORG), meq(LocalDate.of(1997, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CTC_ORG), meq(LocalDate.of(1997, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))

      val result = service.getEnrolments(UPDATE, oldEori, LocalDate.of(1997, 11, 3)).futureValue
      result.count(_._2 == TRUE) shouldBe 7
    }

    "return only ATAR if Eori enrolled only to ATAR" in {
      val oldEori = Eori("GB123456789001")
      val dateOfEstablishment = "03/11/1998"

      val mockData: Enrolment = Enrolment(
        Seq(KeyValue("EORINumber", oldEori.toString)),
        Seq(KeyValue("DateOfEstablishment", dateOfEstablishment))
      )

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CUS_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_ATAR_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))

      when(mockQueryGroups.query(meq(oldEori), meq(HMRC_ATAR_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(mockGroupIdData)))

      when(mockQueryUsers.query(meq(oldEori), meq(HMRC_ATAR_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(mockUserIdData)))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_SS_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_GVMS_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CTS_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_ESC_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CTC_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      val result = service.getEnrolments(UPDATE, oldEori, LocalDate.of(1998, 11, 3)).futureValue
      result.count(_._2 == TRUE) shouldBe 1
      result.filter(_._2 == TRUE).toList.head._1 shouldBe HMRC_ATAR_ORG.serviceName
    }

    "return GVMS and SS if Eori enrolled" in {
      val oldEori = Eori("GB123456789002")
      val dateOfEstablishment = "03/11/1998"

      val mockData: Enrolment = Enrolment(
        Seq(KeyValue("EORINumber", oldEori.toString)),
        Seq(KeyValue("DateOfEstablishment", dateOfEstablishment))
      )

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CUS_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_ATAR_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_SS_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))

      when(mockQueryGroups.query(meq(oldEori), meq(HMRC_SS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(mockGroupIdData)))

      when(mockQueryUsers.query(meq(oldEori), meq(HMRC_SS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(mockUserIdData)))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_GVMS_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))

      when(mockQueryGroups.query(meq(oldEori), meq(HMRC_GVMS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(mockGroupIdData)))

      when(mockQueryUsers.query(meq(oldEori), meq(HMRC_GVMS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(mockUserIdData)))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CTS_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_ESC_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CTC_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      val result = service.getEnrolments(UPDATE, oldEori, LocalDate.of(1998, 11, 3)).futureValue
      result.count(_._2 == TRUE) shouldBe 2
      result.filter(_._2 == TRUE).toList.map(_._1) shouldBe List(HMRC_GVMS_ORG.serviceName, HMRC_SS_ORG.serviceName)
    }

    "return only GVMS if Eori enrolled" in {
      val oldEori = Eori("GB123456789002")
      val dateOfEstablishment = "03/11/1998"

      val mockData: Enrolment = Enrolment(
        Seq(KeyValue("EORINumber", oldEori.toString)),
        Seq(KeyValue("DateOfEstablishment", dateOfEstablishment))
      )

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CUS_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_ATAR_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_SS_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_GVMS_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))

      when(mockQueryGroups.query(meq(oldEori), meq(HMRC_GVMS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(mockGroupIdData)))

      when(mockQueryUsers.query(meq(oldEori), meq(HMRC_GVMS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(mockUserIdData)))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CTS_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_ESC_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CTC_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      val result = service.getEnrolments(UPDATE, oldEori, LocalDate.of(1998, 11, 3)).futureValue
      result.count(_._2 == TRUE) shouldBe 1
      result.filter(_._2 == TRUE).toList.map(_._1) shouldBe List(HMRC_GVMS_ORG.serviceName)
    }

    "return only GVMS with date not matched with Date is not matched" in {
      val oldEori = Eori("GB123456789002")

      when(mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CUS_ORG), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_ATAR_ORG), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_SS_ORG), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_GVMS_ORG), any())(any[HeaderCarrier]))
        .thenReturn(
          Future.successful(
            Left(ErrorMessage("The date you have entered does not match our records, please try again"))
          )
        )

      when(mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CTS_ORG), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_ESC_ORG), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CTC_ORG), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      val result = service.getEnrolments(UPDATE, oldEori, LocalDate.of(1990, 11, 3)).futureValue
      result.count(_._2 == ESTABLISHMENT_DATE_WRONG) shouldBe 1
      result.filter(_._2 == ESTABLISHMENT_DATE_WRONG).toList.map(_._1) shouldBe List(HMRC_GVMS_ORG.serviceName)
    }

    "return only CTS even CUS has Know Facts (Special Case!!!)" in {
      val oldEori = Eori("GB123456789003")
      val dateOfEstablishment = "03/11/1998"

      val mockData: Enrolment = Enrolment(
        Seq(KeyValue("EORINumber", oldEori.toString)),
        Seq(KeyValue("DateOfEstablishment", dateOfEstablishment))
      )

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CUS_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))

      when(mockQueryGroups.query(meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Group for existing EORI: $oldEori"))))

      when(mockQueryUsers.query(meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Group for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_ATAR_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_SS_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_GVMS_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CTS_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))

      when(mockQueryGroups.query(meq(oldEori), meq(HMRC_CTS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(mockGroupIdData)))

      when(mockQueryUsers.query(meq(oldEori), meq(HMRC_CTS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(mockUserIdData)))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_ESC_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CTC_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      val result = service.getEnrolments(UPDATE, oldEori, LocalDate.of(1998, 11, 3)).futureValue
      result.count(_._2 == TRUE) shouldBe 1
      result.filter(_._2 == TRUE).toList.map(_._1) shouldBe List(HMRC_CTS_ORG.serviceName)
    }

    "return only CUS even CTS has Know Facts (Special Case!!!)" in {
      val oldEori = Eori("GB123456789004")
      val dateOfEstablishment = "03/11/1998"

      val mockData: Enrolment = Enrolment(
        Seq(KeyValue("EORINumber", oldEori.toString)),
        Seq(KeyValue("DateOfEstablishment", dateOfEstablishment))
      )

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CUS_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))

      when(mockQueryGroups.query(meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(mockGroupIdData)))

      when(mockQueryUsers.query(meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(mockUserIdData)))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_ATAR_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_SS_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_GVMS_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CTS_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))

      when(mockQueryGroups.query(meq(oldEori), meq(HMRC_CTS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Group for existing EORI: $oldEori"))))

      when(mockQueryUsers.query(meq(oldEori), meq(HMRC_CTS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Group for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_ESC_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CTC_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      val result = service.getEnrolments(UPDATE, oldEori, LocalDate.of(1998, 11, 3)).futureValue
      result.count(_._2 == TRUE) shouldBe 1
      result.filter(_._2 == TRUE).toList.map(_._1) shouldBe List(HMRC_CUS_ORG.serviceName)
    }

    "return both CUS and CTS if both has group and user (Special Case!!!)" in {
      val oldEori = Eori("GB123456789004")
      val dateOfEstablishment = "03/11/1998"

      val mockData: Enrolment = Enrolment(
        Seq(KeyValue("EORINumber", oldEori.toString)),
        Seq(KeyValue("DateOfEstablishment", dateOfEstablishment))
      )

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CUS_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))

      when(mockQueryGroups.query(meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(mockGroupIdData)))

      when(mockQueryUsers.query(meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(mockUserIdData)))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_ATAR_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_SS_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_GVMS_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CTS_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))

      when(mockQueryGroups.query(meq(oldEori), meq(HMRC_CTS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(mockGroupIdData)))

      when(mockQueryUsers.query(meq(oldEori), meq(HMRC_CTS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(mockUserIdData)))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_ESC_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CTC_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      val result = service.getEnrolments(UPDATE, oldEori, LocalDate.of(1998, 11, 3)).futureValue
      result.count(_._2 == TRUE) shouldBe 2
      result.filter(_._2 == TRUE).toList.map(_._1) shouldBe List(HMRC_CUS_ORG.serviceName, HMRC_CTS_ORG.serviceName)
    }

    "return empty list if there is no enrolment" in {
      val oldEori = Eori("GB123456789005")

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CUS_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_ATAR_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_SS_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_GVMS_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CTS_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_ESC_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CTC_ORG), meq(LocalDate.of(1998, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Known Facts for existing EORI: $oldEori"))))

      val result = service.getEnrolments(UPDATE, oldEori, LocalDate.of(1998, 11, 3)).futureValue
      result.count(_._2 == TRUE) shouldBe 0
    }
  }

  "Update Eori" should {
    "update enrolments if existing EORI is found by the enrolment service" in {
      val oldEori = Eori("GB123456789005")
      val newEori = Eori("GB9866255332")
      val dateOfEstablishment = "03/11/1997"

      val mockData: Enrolment = Enrolment(
        Seq(KeyValue("EORINumber", oldEori.toString)),
        Seq(KeyValue("DateOfEstablishment", dateOfEstablishment))
      )

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CUS_ORG), meq(LocalDate.of(1997, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))
      when(mockQueryUsers.query(meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(UserId("0012236665"))))
      when(mockQueryGroups.query(meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(GroupId("90ccf333-65d2-4bf2-a008-abc23783"))))

      when(mockUpsertKnownFacts.upsert(meq(newEori), meq(HMRC_CUS_ORG), meq(mockData))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      when(
        mockDeAllocateGroup.deAllocateGroup(
          meq(UPDATE),
          meq(oldEori),
          meq(HMRC_CUS_ORG),
          meq(GroupId("90ccf333-65d2-4bf2-a008-abc23783"))
        )(any[HeaderCarrier])
      )
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      when(
        mockReAllocateGroup.reAllocate(
          meq(newEori),
          meq(HMRC_CUS_ORG),
          meq(UserId("0012236665")),
          meq(GroupId("90ccf333-65d2-4bf2-a008-abc23783"))
        )(any[HeaderCarrier])
      )
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      when(mockRemoveKnownFacts.remove(meq(UPDATE), meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      when(mockCustomsDataStore.notify(meq(newEori))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      val result = service.update(oldEori, LocalDate.of(1997, 11, 3), newEori, HMRC_CUS_ORG).futureValue
      result shouldBe Right(mockData)
    }

    "update enrolments if existing EORI is found by the enrolment service and notify return 404" in {
      val oldEori = Eori("GB123456789005")
      val newEori = Eori("GB9866255332")
      val dateOfEstablishment = "03/11/1997"

      val mockData: Enrolment = Enrolment(
        Seq(KeyValue("EORINumber", oldEori.toString)),
        Seq(KeyValue("DateOfEstablishment", dateOfEstablishment))
      )

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CUS_ORG), meq(LocalDate.of(1997, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))
      when(mockQueryUsers.query(meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(UserId("0012236665"))))
      when(mockQueryGroups.query(meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(GroupId("90ccf333-65d2-4bf2-a008-abc23783"))))

      when(mockUpsertKnownFacts.upsert(meq(newEori), meq(HMRC_CUS_ORG), meq(mockData))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      when(
        mockDeAllocateGroup.deAllocateGroup(
          meq(UPDATE),
          meq(oldEori),
          meq(HMRC_CUS_ORG),
          meq(GroupId("90ccf333-65d2-4bf2-a008-abc23783"))
        )(any[HeaderCarrier])
      )
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      when(
        mockReAllocateGroup.reAllocate(
          meq(newEori),
          meq(HMRC_CUS_ORG),
          meq(UserId("0012236665")),
          meq(GroupId("90ccf333-65d2-4bf2-a008-abc23783"))
        )(any[HeaderCarrier])
      )
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      when(mockRemoveKnownFacts.remove(meq(UPDATE), meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      when(mockCustomsDataStore.notify(meq(newEori))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(NOT_FOUND)))

      val result = service.update(oldEori, LocalDate.of(1997, 11, 3), newEori, HMRC_CUS_ORG).futureValue
      result shouldBe Right(mockData)
    }

    "return Error Message if there is no user Id" in {
      val oldEori = Eori("GB123456789005")
      val newEori = Eori("GB9866255332")
      val dateOfEstablishment = "03/11/1997"

      val mockData: Enrolment = Enrolment(
        Seq(KeyValue("EORINumber", oldEori.toString)),
        Seq(KeyValue("DateOfEstablishment", dateOfEstablishment))
      )

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CUS_ORG), meq(LocalDate.of(1997, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))
      when(mockQueryUsers.query(meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find User for existing EORI: $oldEori"))))

      val result = service.update(oldEori, LocalDate.of(1997, 11, 3), newEori, HMRC_CUS_ORG).futureValue
      result shouldBe Left(ErrorMessage(s"Could not find User for existing EORI: $oldEori"))
    }

    "return Error Message if there is no group Id" in {
      val oldEori = Eori("GB123456789005")
      val newEori = Eori("GB9866255332")
      val dateOfEstablishment = "03/11/1997"

      val mockData: Enrolment = Enrolment(
        Seq(KeyValue("EORINumber", oldEori.toString)),
        Seq(KeyValue("DateOfEstablishment", dateOfEstablishment))
      )

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CUS_ORG), meq(LocalDate.of(1997, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))
      when(mockQueryUsers.query(meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(UserId("0012236665"))))
      when(mockQueryGroups.query(meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Group for existing EORI: $oldEori"))))

      val result = service.update(oldEori, LocalDate.of(1997, 11, 3), newEori, HMRC_CUS_ORG).futureValue
      result shouldBe Left(ErrorMessage(s"Could not find Group for existing EORI: $oldEori"))
    }

    "return Error Message if upsert known facts to new number fails" in {
      val oldEori = Eori("GB123456789005")
      val newEori = Eori("GB9866255332")
      val dateOfEstablishment = "03/11/1997"

      val mockData: Enrolment = Enrolment(
        Seq(KeyValue("EORINumber", oldEori.toString)),
        Seq(KeyValue("DateOfEstablishment", dateOfEstablishment))
      )

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CUS_ORG), meq(LocalDate.of(1997, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))
      when(mockQueryUsers.query(meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(UserId("0012236665"))))
      when(mockQueryGroups.query(meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(GroupId("90ccf333-65d2-4bf2-a008-abc23783"))))

      when(mockUpsertKnownFacts.upsert(meq(newEori), meq(HMRC_CUS_ORG), meq(mockData))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(ErrorMessage(s"Upsert failed with HTTP status: 500"))))

      val result = service.update(oldEori, LocalDate.of(1997, 11, 3), newEori, HMRC_CUS_ORG).futureValue
      result shouldBe Left(ErrorMessage(s"Upsert failed with HTTP status: 500"))
    }

    "return Error Message if de allocate from group fails" in {
      val oldEori = Eori("GB123456789005")
      val newEori = Eori("GB9866255332")
      val dateOfEstablishment = "03/11/1997"

      val mockData: Enrolment = Enrolment(
        Seq(KeyValue("EORINumber", oldEori.toString)),
        Seq(KeyValue("DateOfEstablishment", dateOfEstablishment))
      )

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CUS_ORG), meq(LocalDate.of(1997, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))
      when(mockQueryUsers.query(meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(UserId("0012236665"))))
      when(mockQueryGroups.query(meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(GroupId("90ccf333-65d2-4bf2-a008-abc23783"))))

      when(mockUpsertKnownFacts.upsert(meq(newEori), meq(HMRC_CUS_ORG), meq(mockData))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      when(
        mockDeAllocateGroup.deAllocateGroup(
          meq(UPDATE),
          meq(oldEori),
          meq(HMRC_CUS_ORG),
          meq(GroupId("90ccf333-65d2-4bf2-a008-abc23783"))
        )(any[HeaderCarrier])
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Delete enrolment failed with HTTP status: 500"))))

      val result = service.update(oldEori, LocalDate.of(1997, 11, 3), newEori, HMRC_CUS_ORG).futureValue
      result shouldBe Left(ErrorMessage(s"Delete enrolment failed with HTTP status: 500"))
    }

    "return Error Message if re allocate to group fails" in {
      val oldEori = Eori("GB123456789005")
      val newEori = Eori("GB9866255332")
      val dateOfEstablishment = "03/11/1997"

      val mockData: Enrolment = Enrolment(
        Seq(KeyValue("EORINumber", oldEori.toString)),
        Seq(KeyValue("DateOfEstablishment", dateOfEstablishment))
      )

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CUS_ORG), meq(LocalDate.of(1997, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))
      when(mockQueryUsers.query(meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(UserId("0012236665"))))
      when(mockQueryGroups.query(meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(GroupId("90ccf333-65d2-4bf2-a008-abc23783"))))

      when(mockUpsertKnownFacts.upsert(meq(newEori), meq(HMRC_CUS_ORG), meq(mockData))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      when(
        mockDeAllocateGroup.deAllocateGroup(
          meq(UPDATE),
          meq(oldEori),
          meq(HMRC_CUS_ORG),
          meq(GroupId("90ccf333-65d2-4bf2-a008-abc23783"))
        )(any[HeaderCarrier])
      )
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      when(mockRemoveKnownFacts.remove(meq(UPDATE), meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      when(
        mockReAllocateGroup.reAllocate(
          meq(newEori),
          meq(HMRC_CUS_ORG),
          meq(UserId("0012236665")),
          meq(GroupId("90ccf333-65d2-4bf2-a008-abc23783"))
        )(any[HeaderCarrier])
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Allocate group failed with HTTP status: 500 (Some Text)"))))

      val result = service.update(oldEori, LocalDate.of(1997, 11, 3), newEori, HMRC_CUS_ORG).futureValue
      result shouldBe Left(ErrorMessage(s"Allocate group failed with HTTP status: 500 (Some Text)"))
    }

    "return Error Message if remove known facts of old number fails" in {
      val oldEori = Eori("GB123456789005")
      val newEori = Eori("GB9866255332")
      val dateOfEstablishment = "03/11/1997"

      val mockData: Enrolment = Enrolment(
        Seq(KeyValue("EORINumber", oldEori.toString)),
        Seq(KeyValue("DateOfEstablishment", dateOfEstablishment))
      )

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CUS_ORG), meq(LocalDate.of(1997, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))
      when(mockQueryUsers.query(meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(UserId("0012236665"))))
      when(mockQueryGroups.query(meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(GroupId("90ccf333-65d2-4bf2-a008-abc23783"))))

      when(mockUpsertKnownFacts.upsert(meq(newEori), meq(HMRC_CUS_ORG), meq(mockData))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      when(
        mockDeAllocateGroup.deAllocateGroup(
          meq(UPDATE),
          meq(oldEori),
          meq(HMRC_CUS_ORG),
          meq(GroupId("90ccf333-65d2-4bf2-a008-abc23783"))
        )(any[HeaderCarrier])
      )
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      when(
        mockReAllocateGroup.reAllocate(
          meq(newEori),
          meq(HMRC_CUS_ORG),
          meq(UserId("0012236665")),
          meq(GroupId("90ccf333-65d2-4bf2-a008-abc23783"))
        )(any[HeaderCarrier])
      )
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      when(mockRemoveKnownFacts.remove(meq(UPDATE), meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(ErrorMessage(s"Remove known facts failed with HTTP status: 500"))))

      val result = service.update(oldEori, LocalDate.of(1997, 11, 3), newEori, HMRC_CUS_ORG).futureValue
      result shouldBe Left(ErrorMessage(s"Remove known facts failed with HTTP status: 500"))
    }

    "return Error Message if notify fails" in {
      val oldEori = Eori("GB123456789005")
      val newEori = Eori("GB9866255332")
      val dateOfEstablishment = "03/11/1997"

      val mockData: Enrolment = Enrolment(
        Seq(KeyValue("EORINumber", oldEori.toString)),
        Seq(KeyValue("DateOfEstablishment", dateOfEstablishment))
      )

      when(
        mockQueryKnownFacts.query(meq(UPDATE), meq(oldEori), meq(HMRC_CUS_ORG), meq(LocalDate.of(1997, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))
      when(mockQueryUsers.query(meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(UserId("0012236665"))))
      when(mockQueryGroups.query(meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(GroupId("90ccf333-65d2-4bf2-a008-abc23783"))))

      when(mockUpsertKnownFacts.upsert(meq(newEori), meq(HMRC_CUS_ORG), meq(mockData))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      when(
        mockDeAllocateGroup.deAllocateGroup(
          meq(UPDATE),
          meq(oldEori),
          meq(HMRC_CUS_ORG),
          meq(GroupId("90ccf333-65d2-4bf2-a008-abc23783"))
        )(any[HeaderCarrier])
      )
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      when(
        mockReAllocateGroup.reAllocate(
          meq(newEori),
          meq(HMRC_CUS_ORG),
          meq(UserId("0012236665")),
          meq(GroupId("90ccf333-65d2-4bf2-a008-abc23783"))
        )(any[HeaderCarrier])
      )
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      when(mockRemoveKnownFacts.remove(meq(UPDATE), meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      when(mockCustomsDataStore.notify(meq(newEori))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(ErrorMessage(s"Notification failed with HTTP status: 500"))))

      val result = service.update(oldEori, LocalDate.of(1997, 11, 3), newEori, HMRC_CUS_ORG).futureValue
      result shouldBe Left(ErrorMessage(s"Notification failed with HTTP status: 500"))
    }

  }

  "Cancel Eori" should {
    "cancel enrolments if existing EORI is found by the enrolment service" in {
      val oldEori = Eori("GB123456789006")
      val dateOfEstablishment = "03/11/1997"

      val mockData: Enrolment = Enrolment(
        Seq(KeyValue("EORINumber", oldEori.toString)),
        Seq(KeyValue("DateOfEstablishment", dateOfEstablishment))
      )

      when(
        mockQueryKnownFacts.query(meq(CANCEL), meq(oldEori), meq(HMRC_CUS_ORG), meq(LocalDate.of(1997, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))
      when(mockQueryGroups.query(meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(GroupId("90ccf333-65d2-4bf2-a008-abc23783"))))

      when(
        mockDeAllocateGroup.deAllocateGroup(
          meq(CANCEL),
          meq(oldEori),
          meq(HMRC_CUS_ORG),
          meq(GroupId("90ccf333-65d2-4bf2-a008-abc23783"))
        )(any[HeaderCarrier])
      )
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      when(mockRemoveKnownFacts.remove(meq(CANCEL), meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      val result = service.cancel(oldEori, LocalDate.of(1997, 11, 3), HMRC_CUS_ORG).futureValue
      result shouldBe Right(mockData)
    }

    "cancel ATAR enrolments if existing EORI is found by the enrolment service" in {
      val oldEori = Eori("GB123456789006")
      val dateOfEstablishment = "03/11/1997"

      val mockData: Enrolment = Enrolment(
        Seq(KeyValue("EORINumber", oldEori.toString)),
        Seq(KeyValue("DateOfEstablishment", dateOfEstablishment))
      )

      when(
        mockQueryKnownFacts.query(meq(CANCEL), meq(oldEori), meq(HMRC_ATAR_ORG), meq(LocalDate.of(1997, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))
      when(mockQueryGroups.query(meq(oldEori), meq(HMRC_ATAR_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(GroupId("90ccf333-65d2-4bf2-a008-abc23783"))))

      when(
        mockDeAllocateGroup.deAllocateGroup(
          meq(CANCEL),
          meq(oldEori),
          meq(HMRC_ATAR_ORG),
          meq(GroupId("90ccf333-65d2-4bf2-a008-abc23783"))
        )(any[HeaderCarrier])
      )
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      when(mockRemoveKnownFacts.remove(meq(CANCEL), meq(oldEori), meq(HMRC_ATAR_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      val result = service.cancel(oldEori, LocalDate.of(1997, 11, 3), HMRC_ATAR_ORG).futureValue
      result shouldBe Right(mockData)
    }

    "return Error Message if there is no group Id" in {
      val oldEori = Eori("GB123456789006")
      val dateOfEstablishment = "03/11/1997"

      val mockData: Enrolment = Enrolment(
        Seq(KeyValue("EORINumber", oldEori.toString)),
        Seq(KeyValue("DateOfEstablishment", dateOfEstablishment))
      )

      when(
        mockQueryKnownFacts.query(meq(CANCEL), meq(oldEori), meq(HMRC_CUS_ORG), meq(LocalDate.of(1997, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))

      when(mockQueryGroups.query(meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(ErrorMessage(s"Could not find Group for existing EORI: $oldEori"))))

      val result = service.cancel(oldEori, LocalDate.of(1997, 11, 3), HMRC_CUS_ORG).futureValue
      result shouldBe Left(ErrorMessage(s"Could not find Group for existing EORI: $oldEori"))
    }

    "return Error Message if de allocate from group fails" in {
      val oldEori = Eori("GB123456789006")
      val dateOfEstablishment = "03/11/1997"

      val mockData: Enrolment = Enrolment(
        Seq(KeyValue("EORINumber", oldEori.toString)),
        Seq(KeyValue("DateOfEstablishment", dateOfEstablishment))
      )

      when(
        mockQueryKnownFacts.query(meq(CANCEL), meq(oldEori), meq(HMRC_CUS_ORG), meq(LocalDate.of(1997, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))

      when(mockQueryGroups.query(meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(GroupId("90ccf333-65d2-4bf2-a008-abc23783"))))

      when(
        mockDeAllocateGroup.deAllocateGroup(
          meq(CANCEL),
          meq(oldEori),
          meq(HMRC_CUS_ORG),
          meq(GroupId("90ccf333-65d2-4bf2-a008-abc23783"))
        )(any[HeaderCarrier])
      )
        .thenReturn(Future.successful(Left(ErrorMessage(s"Delete enrolment failed with HTTP status: 500"))))

      val result = service.cancel(oldEori, LocalDate.of(1997, 11, 3), HMRC_CUS_ORG).futureValue
      result shouldBe Left(ErrorMessage(s"Delete enrolment failed with HTTP status: 500"))
    }

    "return Error Message if remove known facts fails" in {
      val oldEori = Eori("GB123456789006")
      val dateOfEstablishment = "03/11/1997"

      val mockData: Enrolment = Enrolment(
        Seq(KeyValue("EORINumber", oldEori.toString)),
        Seq(KeyValue("DateOfEstablishment", dateOfEstablishment))
      )

      when(
        mockQueryKnownFacts.query(meq(CANCEL), meq(oldEori), meq(HMRC_CUS_ORG), meq(LocalDate.of(1997, 11, 3)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Right(mockData)))

      when(mockQueryGroups.query(meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(GroupId("90ccf333-65d2-4bf2-a008-abc23783"))))

      when(
        mockDeAllocateGroup.deAllocateGroup(
          meq(CANCEL),
          meq(oldEori),
          meq(HMRC_CUS_ORG),
          meq(GroupId("90ccf333-65d2-4bf2-a008-abc23783"))
        )(any[HeaderCarrier])
      )
        .thenReturn(Future.successful(Right(NO_CONTENT)))

      when(mockRemoveKnownFacts.remove(meq(CANCEL), meq(oldEori), meq(HMRC_CUS_ORG))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(ErrorMessage(s"Remove known facts failed with HTTP status: 500"))))

      val result = service.cancel(oldEori, LocalDate.of(1997, 11, 3), HMRC_CUS_ORG).futureValue
      result shouldBe Left(ErrorMessage(s"Remove known facts failed with HTTP status: 500"))
    }
  }
}
