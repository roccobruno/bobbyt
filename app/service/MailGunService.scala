package service

import java.net.ConnectException
import java.util.concurrent.TimeoutException

import model.{Converters, MailgunId, MailgunSendResponse, EmailToSent}
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
      ws.url(mailGunUrl).withHeaders((HeaderNames.AUTHORIZATION, authValue)).
        post(Converters.emailToFormBody(emailToSent)) map {
        response =>
          response.status match {
            case status if is2xx(status) => response.json.as[MailgunSendResponse]
            case 400 => println(s"MailGun Request fails with response 400. Check the parameters passed - ${response.json}")
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
      Future.successful(MailgunSendResponse(MailgunId("NO-ID"),""))
    }
  }

  def badGatewayMessage(verbName: String, url: String, e: Exception): String = {
    s"$verbName of '$url' failed. Caused by: '${e.getMessage}'"
  }

  def gatewayTimeoutMessage(verbName: String, url: String, e: Exception): String = {
    s"$verbName of '$url' timed out with message '${e.getMessage}'"
  }


  def emailTemplate(nameFrom: String, nameTo: String, emailFrom: String) =
    s"""
      |<html>
      |
      |	<head>
      |
      |		<title>Email Content</title>
      |
      |		<!-- Normalize.css -->
      |        <link rel="stylesheet" type="text/css" href="https://s3-eu-west-1.amazonaws.com/bobbit/email/normalize.css">
      |
      |        <!-- Custom Styles -->
      |        <link rel="stylesheet" type="text/css" href="https://s3-eu-west-1.amazonaws.com/bobbit/email/styles.css">
      |
      |        <!--[if lt IE]>
      |  <script src="http://html5shiv.googlecode.com/svn/trunk/html5.js"></script>
      |  <![endif]-->
      |
      |	</head>
      |
      |	<body>
      |
      |		<header><a href="www.bobbit.co.uk" id="mainLink"></a></header>
      |
      |		<div id="container">
      |
      |						<p id="hello">Hi $nameTo,</p>
      |
      |				<div id="emailContent">
      |
      |						<p>Due to <em>severe delays</em> on the Central line, $nameFrom might be few minutes late at work this morning.</p>
      |						<p>This message was sent by $nameFrom using <a href="www.bobbit.co.uk" id="link1">Bobbit</a>.</p>
      |
      |				</div>
      |				<div id="signature">
      |						Kind regards, <br> Bobbit Team
      |				</div>
      |
      |				<div id="line1"></div>
      |				<div id="line2"></div>
      |				<div id="line3"></div>
      |
      |		</div><!-- end #container -->
      |
      |		<footer id="footer">
      |                <p>&copy; 2016 &bull; Bobbit</p>
      |
      |				<small>Message sent by $nameFrom (e-mail: $emailFrom) using <a href="www.bobbit.co.uk" class="links">bobbit.co.uk</a> services.<br>For more information about Tube Lines updates please check <a href="https://tfl.gov.uk" class="links">here.</a></small>
      |
      |        </footer>
      |
      |	</body>
      |
      |
      |
      |
      |
      |</html>
    """.stripMargin


  def newTemplate(nameFrom: String, nameTo: String, emailFrom: String) =
    """
      |
      |<!DOCTYPE html>
      |<html>
      |
      |	<head>
      |
      |		<title>Email Content</title>
      |
      |		<!-- Internal CSS for Email -->
      |		<style type="text/css">
      |
      |
      |					body {
      |					font-size: 14px;
      |					color: #3d3d29;
      |					text-align: justify;
      |					font-family: Verdana, Geneva, sans-serif;
      |					background: #b8b894;
      |					line-height: 150%;
      |
      |				}
      |
      |				header {
      |					font-size: 25px;
      |					text-align: center;
      |					text-shadow: 1px 2px 5px #333;
      |					font-weight: 800;
      |					color: white;
      |					background: #b8b894;
      |					padding: 20px 0;
      |					margin: 0 auto;
      |					width: 100%;
      |        position:relative;
      |				}
      |
      |				em {
      |					font-weight: 600;
      |					font-style: normal;
      |				}
      |
      |				#container {
      |					background: white;
      |					width: 50%;
      |					height: 50%;
      |					margin: 0 auto;
      |					margin-bottom: 10px;
      |					padding: 30px;
      |					box-sizing: border-box;
      |					box-shadow: 0 10px 25px -8px #4d4d33;
      |					border-radius: 8px;
      |
      |				}
      |
      |				#signature {
      |					font-weight: 600;
      |					line-height: 150%;
      |					margin-top: 40px;
      |				}
      |
      |				#line1 {
      |					margin-top: 200px;
      |					border: solid 2px #8a8a5c;
      |				}
      |
      |				#line2 {
      |					margin:5px 0;
      |					border: solid 3px #6b6b47;
      |				}
      |
      |				#line3 {
      |					margin:5px 0;
      |					border: solid 4px #5c5c3d;
      |				}
      |
      |				a#mainLink:link {
      |					color: #fff;
      |					text-decoration: none;
      |				}
      |
      |				a.links:link {
      |					color: #3d3d29;
      |					text-decoration: none;
      |					font-weight: bold;
      |				}
      |
      |				a.links:visited {
      |					color: #3d3d29;
      |				}
      |
      |				a#link1:link {
      |					color: #4d4d33;
      |					font-family: Verdana, Geneva, sans-serif;
      |				}
      |
      |				#footer {
      |					text-align: center;
      |					margin: auto 0;
      |					background: #c1c1a4;
      |					position: fixed;
      |					bottom: 0;
      |					width: 100%;
      |     position:relative;
      |				}
      |
      |				footer p {
      |					font-weight: bold;
      |				}
      |
      |
      |
      |		</style>
      |	</head>
      |
      |	<body>
      |
      |		<header><a href="www.bobbit.co.uk" id="mainLink"></a></header>
      |
      |		<div id="container">
      |
      |						<p id="hello">Hi Rocco,</p>
      |
      |				<div id="emailContent">
      |
      |						<p>Due to <em>severe delays</em> on the Central line, Orjana Veizaj might be few minutes late at work this morning.</p>
      |						<p>This message was sent by Orjana Veizaj using <a href="www.bobbit.co.uk" id="link1">Bobbit</a>.</p>
      |
      |				</div>
      |				<div id="signature">
      |						Kind regards, <br> Bobbit Team
      |				</div>
      |
      |				<div id="line1"></div>
      |				<div id="line2"></div>
      |				<div id="line3"></div>
      |
      |		</div><!-- end #container -->
      |
      |		<footer id="footer">
      |                <p>&copy; 2016 &bull; Bobbit</p>
      |
      |				<small>Message sent by Orjana Veizaj (e-mail: oriana@testing.com) using <a href="www.bobbit.co.uk" class="links">bobbit.co.uk</a> services.<br>For more information about Tube Lines updates please check <a href="https://tfl.gov.uk" class="links">here.</a></small>
      |
      |        </footer>
      |
      |	</body>
      |
      |
      |
      |
      |
      |</html>
    """.stripMargin

}


