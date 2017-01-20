package model

import play.api.libs.json._

case class MailgunSendResponse(id: MailgunId, message: String)

object MailgunSendResponse {
  implicit val formats = {
    implicit val mailgunIdReads = MailgunId.reads
    Json.reads[MailgunSendResponse]
  }
}