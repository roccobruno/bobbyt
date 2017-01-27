package service

import model.{Converters, EmailToSent, MailgunId, MailgunSendResponse}
import org.apache.commons.codec.binary.Base64
import org.mockito.Mockito
import org.mockito.Mockito.{mock, when}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import play.api.Configuration
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import util.Testing
import scala.concurrent.duration._
import org.mockito.Matchers.any;

import scala.concurrent.{Await, Future}

class MailGunServiceSpec extends WordSpecLike with Matchers with BeforeAndAfterAll {

  "a service" should {



    "return MailGunResponse with ID" in {

      val confMock = mock(classOf[Configuration])
      val wsCLientMock = mock(classOf[WSClient])

      when(confMock.getString("mailgun-api-key")).thenReturn(Some("KEY"))
      when(confMock.getString("mailgun-host")).thenReturn(Some("host"))
      when(confMock.getBoolean("mailgun-enabled")).thenReturn(Some(true))

      val emailToSent = EmailToSent("test@test.it", "to@test.it", "ciao", Some("test"), None)

      val mockResponse = mock(classOf[WSResponse])

      val authValue = s"Basic ${Base64.encodeBase64String(s"api:KEY".getBytes("UTF-8"))}"

      val mockedRequest = mock(classOf[WSRequest])
      val mockedRequest2 = mock(classOf[WSRequest])
      when(mockedRequest.withHeaders(HeaderNames.AUTHORIZATION -> authValue)).thenReturn(mockedRequest2)
      when(mockedRequest2.post(any[Map[String,Seq[String]]]())(any())).thenReturn(Future.successful(mockResponse))

      when(wsCLientMock.url("host")).thenReturn(mockedRequest)

      when(mockResponse.status).thenReturn(200)
      val id: MailgunId = MailgunId("id")
      val json =
        """
           {
           "id": "id",
           "message":"success"
           }
        """.stripMargin
      when(mockResponse.json).thenReturn(Json.parse(json))

      val service = new MailGunService(confMock, wsCLientMock)
      val res = Await.result(service.sendEmail(emailToSent),10 seconds)

      res.id shouldBe id
      res.message shouldBe "success"

    }



  }



}
