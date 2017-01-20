package model

import play.api.libs.json._

case class MailgunId(id: String) {
  require(!id.startsWith("<"), "Mailgun IDs may not start with chevrons")
  require(!id.endsWith(">"), "Mailgun IDs may not end with chevrons")

  override def toString = id
}

object MailgunId {

  val withoutChevrons = (id: String) => id.replaceAll(">", "").replaceAll("<", "")

  implicit val reads = __.read[String].map(id => MailgunId(withoutChevrons(id)))
}
