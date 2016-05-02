package model

import play.api.libs.json._

case class MailgunSendResponse(id: MailgunId, message: String, sent: Boolean = true)

object MailgunSendResponse {
  implicit val formats = {
    implicit val mailgunIdWrites = MailgunId.writes
    implicit val mailgunIdReads = MailgunId.reads
    Json.format[MailgunSendResponse]
  }
}