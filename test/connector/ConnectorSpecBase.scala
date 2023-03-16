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

package connector

import audit.Auditable
import config.AppConfig
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

class ConnectorSpecBase
  extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with MockitoSugar
    with BeforeAndAfterEach {

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(5, Seconds))
  protected implicit val mockHeaderCarrier = mock[HeaderCarrier]
  protected val mockHttpClient = mock[HttpClient]
  protected val mockAppConfig = mock[AppConfig]
  protected val mockAuditable = mock[Auditable]

  override def beforeEach(): Unit = {
    reset(mockAppConfig, mockHttpClient)

    when(mockAppConfig.enrolmentStoreProxyServiceUrl)
      .thenReturn("http://localhost:1234")

    when(mockAppConfig.taxEnrolmentsServiceUrl)
      .thenReturn("http://localhost:1222")

    when(mockAppConfig.customsDataStoreUrl)
      .thenReturn("http://localhost:1111")
  }
}
