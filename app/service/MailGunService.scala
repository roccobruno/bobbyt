package service

import java.net.ConnectException
import java.util.concurrent.TimeoutException

import model.{MailgunId, MailgunSendResponse, EmailToSent}
import org.apache.commons.codec.binary.Base64
import play.api.Logger
import play.api.http.{Writeable, HeaderNames}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait MailGunService {

  val ws: WSClient

  def mailGunApiKey: String

  def mailGunHost: String

  def enableSender: Boolean

  private def authValue = s"Basic ${Base64.encodeBase64String(s"api:$mailGunApiKey".getBytes("UTF-8"))}"

  val mailGunUrl: String = mailGunHost

  import EmailToSent.format

  def is2xx(status: Int) = status >= 200 && status < 300

  def is4xx(status: Int) = status >= 400 && status < 500

  def is5xx(status: Int) = status >= 500 && status < 600

  def sendEmail(emailToSent: EmailToSent): Future[MailgunSendResponse] = {
    if (enableSender) {
      val json = Json.toJson(emailToSent.copy(subject = Some("TEST")))
      ws.url(mailGunUrl).withHeaders((HeaderNames.AUTHORIZATION, authValue)).post(json) map {
        response =>
          response.status match {
            case status if is2xx(status) => response.json.as[MailgunSendResponse]
            case 400 => println("MailGun Request fails with response 400. Check the parameters passed")
              throw new Exception("MailGun request failed. Either the APIs are changed" +
                "or the request sent is wrong.")
            case 401 => println("MailGun Request fails with response 401. Check the api toker key used"); throw new Exception("MailGun request failed. Either the api key is wrong or it is expired")
            case 402 => println("MailGun Request fails with response 402. Try again"); throw new Exception("MailGun request failed. Try again")
            case status if is4xx(status) => println(s"MailGun Request fails with response $status. Unknown status"); throw new Exception(s"MailGun request failed with unknown status: $status. Try again")
            case status if is5xx(status) => println(s"MailGun Request fails with response $status"); throw new Exception("MailGun request failed with server error. Try again but later")
            case status => throw new Exception(s"MailGun Request fails: to $mailGunUrl failed with status $status. Response body: '${response.body}'")
          }
      } recover {
        case e: TimeoutException => println(s"MailGun Request fails with connection timeout: ${e.printStackTrace()}"); throw new Exception(gatewayTimeoutMessage("POST", mailGunUrl, e))
        case e: ConnectException => println(s"MailGun Request fails with connection problem: ${e.printStackTrace()}"); throw new Exception(badGatewayMessage("POST", mailGunUrl, e))
      }
    } else {
      Logger.info("MailGunService not enabled!!!... emails won't be sent out")
      Future.successful(MailgunSendResponse(MailgunId("NO-ID"),"",false))
    }
  }

  def badGatewayMessage(verbName: String, url: String, e: Exception): String = {
    s"$verbName of '$url' failed. Caused by: '${e.getMessage}'"
  }

  def gatewayTimeoutMessage(verbName: String, url: String, e: Exception): String = {
    s"$verbName of '$url' timed out with message '${e.getMessage}'"
  }

}


