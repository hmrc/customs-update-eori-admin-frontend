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

package audit

import play.api.Configuration
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{Audit, DataEvent}

import javax.inject.{Inject, Singleton}

@Singleton
class Auditor @Inject()(auditConnector: AuditConnector, config: Configuration) {
  private val auditSource: String = config.get[String]("appName")
  private val audit: Audit = Audit(auditSource, auditConnector)

  def sendDataEvent(transactionName: String,
                    path: String = "N/A",
                    detail: Map[String, String],
                    auditType: String)(implicit hc: HeaderCarrier): Unit =
    audit.sendDataEvent(
      DataEvent(auditSource,
        auditType,
        tags = hc.toAuditTags(transactionName, path),
        detail = hc.toAuditDetails(detail.toSeq: _*)))
}
