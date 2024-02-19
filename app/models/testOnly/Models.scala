/*
 * Copyright 2024 HM Revenue & Customs
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

package models.testOnly

import play.api.libs.json.{Json, OFormat, OWrites, Reads}

case class User(
  credId: String,
  name: String = "Default User",
  email: String = "default@example.com",
  credentialRole: String = "Admin",
  description: Option[String] = Some("User Description"),
  owningUserId: Option[String] = None,
  credentialCreatedDate: Option[String] = None,
  lastSuccessfulAuthentication: Option[String] = None,
  lastUnsuccessfulAuthentication: Option[String] = None,
  lastPasswordChange: Option[String] = None,
  accountLocked: Option[String] = None,
  accountLockedExpiry: Option[String] = None,
  suspended: Option[Boolean] = None
)
object User {
  implicit val reads: Reads[User] = Json.reads[User]
  implicit val writes: OWrites[User] = Json.writes[User]
}

case class KnownFact(key: String, value: String, kfType: String)
object KnownFact {
  implicit val reads: Reads[KnownFact] = Json.reads[KnownFact]
  implicit val writes: OWrites[KnownFact] = Json.writes[KnownFact]
}

case class Identifier(key: String, value: String, maskedValue: Option[String] = None)
object Identifier {
  implicit val reads: Reads[Identifier] = Json.reads[Identifier]
  implicit val writes: OWrites[Identifier] = Json.writes[Identifier]
}

case class Enrolment(
  serviceName: String,
  identifiers: Seq[Identifier],
  enrolmentFriendlyName: Option[String] = Some("Customs Enrolment"),
  assignedUserCreds: Seq[String] = Seq.empty,
  state: String = "Activated",
  enrolmentType: String = "principal",
  assignedToAll: Boolean = true,
  enrolmentTokenExpiryDate: Option[String] = None
)

object Enrolment {
  implicit val reads: Reads[Enrolment] = Json.reads[Enrolment]
  implicit val writes: OWrites[Enrolment] = Json.writes[Enrolment]
}

case class GroupPersona(
  groupId: String,
  affinityGroup: String = "Organisation",
  users: Seq[User],
  enrolments: Seq[Enrolment],
  agentCode: Option[String] = None,
  agentId: Option[String] = None,
  agentName: Option[String] = None,
  suspended: Option[Boolean] = None
) {}

object GroupPersona {
  implicit val formats: OFormat[GroupPersona] = Json.format[GroupPersona]
}

case class KnownFactPersona(service: String, knownFacts: Seq[KnownFact])

object KnownFactPersona {
  implicit val formats: OFormat[KnownFactPersona] = Json.format[KnownFactPersona]
}
