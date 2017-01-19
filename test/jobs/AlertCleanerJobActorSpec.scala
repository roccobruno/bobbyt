package jobs

import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit}
import model.{Email, EmailAddress, EmailAlert}
import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.ws.WSClient
import repository.{BobbytRepository, ClusterConfiguration, TubeRepository}
import service.{JobService, MailGunService}
import util.Testing

import scala.concurrent.duration._
import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.specs2.runner.JUnitRunner
import play.api.test.WithApplication
import service.tfl.TubeService


@RunWith(classOf[JUnitRunner])
class AlertCleanerJobActorSpec  extends TestKit(ActorSystem("AlertCleanerJobActorSpec")) with Testing  {


  override protected def beforeAll(): Unit = {
   await(bobbytRepository.deleteAllAlerts())
  }

  "an actor " should {

    "delete all the alert sent" in {

      val jbService =  new JobService (
        Mockito.mock(classOf[Configuration]),
        bobbytRepository,
        Mockito.mock(classOf[TubeRepository]),
        Mockito.mock(classOf[MailGunService]),
        Mockito.mock(classOf[TubeService])
      )

      val actor = TestActorRef(new AlertCleanerJobActor(jbService))


      await(bobbytRepository.saveAlert(
        EmailAlert(
          UUID.randomUUID().toString,
          Email("", EmailAddress("test@test.it"),"", EmailAddress("to@to.it")),
          DateTime.now.minusDays(3),
          Some(DateTime.now().minusDays(2)),
          UUID.randomUUID().toString,
          "Alert",
          sent = true)))



      await(bobbytRepository.saveAlert(
        EmailAlert(
          UUID.randomUUID().toString,
          Email("", EmailAddress("test@test.it"),"", EmailAddress("to@to.it")),
          DateTime.now.minusDays(3),
          Some(DateTime.now().minusDays(2)),
          UUID.randomUUID().toString,
          "Alert",
          sent = true)))

      await(bobbytRepository.saveAlert(
        EmailAlert(
          UUID.randomUUID().toString,
          Email("", EmailAddress("test@test.it"),"", EmailAddress("to@to.it")),
          DateTime.now,
          Some(DateTime.now),
          UUID.randomUUID().toString,
          "Alert",
          sent = true)))

      val alerts = await(bobbytRepository.findAllAlert())
      alerts.size shouldBe 3

      implicit val timeout: Timeout = Timeout(10000, TimeUnit.MILLISECONDS)
      val res = actor ! Run("test")


      val alertsAfterActor = await(bobbytRepository.findAllAlert())
      alertsAfterActor.size shouldBe 1




    }


  }


}
