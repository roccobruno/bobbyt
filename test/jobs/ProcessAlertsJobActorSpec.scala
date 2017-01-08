package jobs

import java.util.UUID
import java.util.concurrent.TimeUnit

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

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.duration.Duration


class ProcessAlertsJobActorSpec extends TestKit(ActorSystem("ProcessAlertsJobActorSpec")) with Testing with ImplicitSender {


  val bobbytRepository = BobbytRepository

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

      val jbService =  new JobService {
        override val repo: BobbytRepository = bobbytRepository
        override val mailGunService: MailGunService = new MailGunService {


          override def sendEmail(emailToSent: EmailToSent): Future[MailgunSendResponse] = {

            emailsBuffer +=  emailToSent
            Future.successful(MailgunSendResponse(MailgunId(""),""))

          }

          override def enableSender: Boolean = true

          override def mailGunHost: String = "test"

          override def mailGunApiKey: String = ""

          override val ws: WSClient = Mockito.mock(classOf[WSClient])
        }
        override val ws: WSClient = Mockito.mock(classOf[WSClient])
        override val configuration: Configuration = Mockito.mock(classOf[Configuration])
        override val tubeRepository: TubeRepository = Mockito.mock(classOf[TubeRepository])
      }

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
