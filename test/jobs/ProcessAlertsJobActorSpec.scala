package jobs

import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.util.Timeout
import model._
import org.joda.time.DateTime
import org.mockito.Mockito
import play.api.Configuration
import play.api.libs.ws.WSClient
import repository.{BobbytRepository, TubeRepository}
import service.{JobService, MailGunService}
import util.Testing
import akka.pattern.ask
import org.junit.runner.RunWith
import org.mockito.Mockito.{mock, when}
import org.specs2.runner.JUnitRunner
import service.tfl.TubeService

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class ProcessAlertsJobActorSpec  extends TestKit(ActorSystem("ProcessAlertsJobActorSpec")) with Testing with ImplicitSender {



  val emailsBuffer = ListBuffer.empty[EmailToSent]

  override protected def beforeAll(): Unit = {
   await(bobbytRepository.deleteAllAlerts())
  }

  "an actor " should {

    val nameFrom: String = "Rocco"
    val emailFrom: String = "test@test.it"
    val emailTo: String = "to@to.it"

    def assertChanges(jId: String, emailSent: Int): Unit = {

      val alertsAfterActor = await(bobbytRepository.findAlertByJobIdAndSentValue(jId, sent = true))
      alertsAfterActor.isDefined shouldBe true
      alertsAfterActor.get.sentAt.isDefined shouldBe true
      alertsAfterActor.get.sentAt.get.dayOfMonth() shouldBe DateTime.now.dayOfMonth()
      alertsAfterActor.get.sentAt.get.dayOfYear() shouldBe DateTime.now.dayOfYear()

      emailsBuffer.size shouldBe emailSent
      emailsBuffer(0).from shouldBe nameFrom + " <test@test.it>"
      emailsBuffer(0).to shouldBe emailTo
    }


    "process all the not sent alerts" in {


      class MailGunServiceMock(conf: Configuration, ws: WSClient) extends MailGunService(conf, ws) {
        override def sendEmail(emailToSent: EmailToSent): Future[MailgunSendResponse] = {
          emailsBuffer +=  emailToSent
          Future.successful(MailgunSendResponse(MailgunId(""),""))
        }
      }


      val configurationMock: Configuration = mock(classOf[Configuration])
      when(configurationMock.getString("mailgun-api-key")).thenReturn(Some("test"))
      when(configurationMock.getString("mailgun-host")).thenReturn(Some("test"))
      when(configurationMock.getBoolean("mailgun-enabled")).thenReturn(Some(false))

      val mailGunService: MailGunService = new MailGunServiceMock(configurationMock, mock(classOf[WSClient]))

      val confMock: Configuration = mock(classOf[Configuration])
      Mockito.when(confMock.getInt("job-limit")).thenReturn(Some(3))

      val jbService =  new JobService (
        confMock,
        bobbytRepository,
        mock(classOf[TubeRepository]),
        mailGunService,
        mock(classOf[TubeService])
      )

      val actor = TestActorRef(new ProcessAlertsJobActor(jbService))


      val jobId: String = UUID.randomUUID().toString
      val jobId2: String = UUID.randomUUID().toString
      val jobIds = Seq(jobId, jobId2)


      var count = 0
      jobIds foreach {
        jId =>
          await(bobbytRepository.saveAlert(
            EmailAlert(
              UUID.randomUUID().toString,
              Email(nameFrom, EmailAddress(emailFrom),"", EmailAddress(emailTo)),
              DateTime.now.minusDays(3),
              None,
              jId,
              "Alert",
              sent = false)))

          count+=1

          val alerts = await(bobbytRepository.findAlertByJobId(jId))
          alerts.size shouldBe 1

          val alertsSent = await(bobbytRepository.findAlertByJobIdAndSentValue(jId, sent = false))
          alertsSent.size shouldBe 1

          implicit val timeout = Timeout(10, TimeUnit.SECONDS)
          val res = actor ! Run("test")


          awaitAssert(assertChanges(jId, count), Duration(10, TimeUnit.SECONDS))
      }











    }


  }


}
