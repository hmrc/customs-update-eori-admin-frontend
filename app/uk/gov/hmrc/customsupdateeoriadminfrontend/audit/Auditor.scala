/*
 * Copyright 2022 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.customsupdateeoriadminfrontend.audit

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
