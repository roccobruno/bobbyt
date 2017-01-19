package jobs

import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.util.Timeout
import model._
import org.joda.time.DateTime
import org.joda.time.DateTime._
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.specs2.runner.JUnitRunner
import play.api.Configuration
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import repository.{BobbytRepository, TubeRepository}
import service.tfl.TubeService
import service.{JobService, MailGunService}
import util.{Testing, TubeLineUtil}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class TubeServiceCheckerJobActorSpec  extends TestKit(ActorSystem("TubeServiceCheckerJobActorSpec"))
  with Testing with ImplicitSender with TubeLineUtil {

  val ttubeRepo = tubeRepository

  val emailsBuffer = ListBuffer.empty[EmailToSent]

  val from = "from@msn.com"
  val to = "to@msn.com"

  override protected def beforeAll(): Unit = {
    await(bobbytRepository.deleteAllAlerts())
    await(bobbytRepository.deleteAllJobs())
  }


  "an actor " should {

    def jobJson (hourOfTheDay: Int, minOfTheHour: Int, jbId: String) = s"""{
                                                                           |  "accountId": "accountId",
                                                                           |  "journey": {
                                                                           |    "meansOfTransportation": {
                                                                           |      "trainService": [],
                                                                           |      "tubeLines": [
                                                                           |        {
                                                                           |          "name": "testLine",
                                                                           |          "id": "testLine"
                                                                           |        }
                                                                           |      ]
                                                                           |    },
                                                                           |    "durationInMin": 40,
                                                                           |    "startsAt": {
                                                                           |      "hour": $hourOfTheDay,
                                                                           |      "min": $minOfTheHour,
                                                                           |      "time": $hourOfTheDay$minOfTheHour
                                                                           |    },
                                                                           |    "recurring": true
                                                                           |  },
                                                                           |  "alert": {
                                                                           |    "nameTo": "name",
                                                                           |    "from": {
                                                                           |      "value": "$from"
                                                                           |    },
                                                                           |    "nameFrom": "name",
                                                                           |    "to": {
                                                                           |      "value": "$to"
                                                                           |    }
                                                                           |  },
                                                                           |  "docType": "Job",
                                                                           |  "active": true,
                                                                           |  "id": "$jbId",
                                                                           |  "title": "jobTile"
                                                                           |}""".stripMargin



    def saveJob(jobId: String, plusHours: Int = 0): Option[String] = {
      val hourOfTheDay: Int = DateTime.now().plusHours(plusHours).hourOfDay().get()
      val minutesOfTheHour: Int = DateTime.now().plusHours(plusHours).minuteOfHour().get()
      val job = Json.fromJson[Job](Json.parse(jobJson(hourOfTheDay, minutesOfTheHour, jobId))).get
      await(bobbytRepository.save(job))

    }
    def checkAlerts: Unit = {
      val alertsCreated = await(bobbytRepository.findAllAlert())
      alertsCreated.size shouldBe 2
      alertsCreated foreach {
        alert =>
          alert.sent shouldBe false
          alert.email.from.value shouldBe from
          alert.email.to.value shouldBe to
      }

    }
    "create alerts in case of tube disraption" in {

      val jbService =  new JobService (
        Mockito.mock(classOf[Configuration]),
        bobbytRepository,
        ttubeRepo,
        Mockito.mock(classOf[MailGunService]),
        Mockito.mock(classOf[TubeService])
      )

      val actor = TestActorRef(new TubeServiceCheckerActor(jbService))


      val jobId: String = UUID.randomUUID().toString
      val jobId2: String = UUID.randomUUID().toString
      val jobId3: String = UUID.randomUUID().toString


      await(tubeRepository.saveTubeService(Seq(tubeLine("testLine"))))
      await(tubeRepository.saveTubeService(Seq(tubeLineNoDisruption("testLineNoDisruption"))))



      saveJob(jobId)
      saveJob(jobId2)
      saveJob(jobId3, plusHours = 6) //this job should not be found (cos it runs in 5 hours)


      val res = actor ! Run("test")

      awaitAssert(checkAlerts, Duration(10, TimeUnit.SECONDS))


      await(tubeRepository.deleteById("testLine"))
      await(tubeRepository.deleteById("testLineNoDisruption"))










    }


  }


}
