package service

import model.{MailgunSendResponse, EmailToSent}
import org.apache.commons.codec.binary.Base64
import play.api.http.{Writeable, HeaderNames}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
trait MailGunService {

  val ws:WSClient
  def mailGunApiKey: String
  def mailGunHost:String

  private def authValue = s"Basic ${Base64.encodeBase64String(s"api:$mailGunApiKey".getBytes("UTF-8"))}"

  val mailGunUrl: String = ""

  import EmailToSent.format

  def sendEmail(emailToSent: EmailToSent) : Future[MailgunSendResponse] = {
    ws.url(mailGunUrl).withHeaders((HeaderNames.AUTHORIZATION, authValue)).post(Json.toJson(emailToSent)) .map(_.json.as[MailgunSendResponse])
  }

}


