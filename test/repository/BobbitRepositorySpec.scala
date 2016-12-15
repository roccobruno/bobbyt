package repository

import java.util.UUID

import model._
import org.joda.time.DateTime
import org.joda.time.DateTime.now
import play.api.libs.json.Json
import play.api.test.WithApplication
import util.Testing

import scala.concurrent.duration._

class BobbitRepositorySpec extends Testing {

  def repo: BobbitRepository = BobbitRepository
  def tubeRepo: TubeRepository = TubeRepository


  override protected def beforeAll(): Unit = {
    await(repo.bucket.dropAllIndexes())
    await(repo.bucket.createPrimaryIndex(deferBuild = false), 10 seconds)
  }

  override protected def afterAll(): Unit = {
    println("shutting down the driver")
    repo.cluster.disconnect()
  }

  "a repository" should {

    "return valid token" in new WithApplication {

      await(repo.deleteAllToken())
      private val token = Token(token = "token", accountId = "accountId")
      await(repo.saveToken(token))

      val res = await(repo.findValidTokenByValue("token"), 10 second)
      res.size should be(1)
      res contains token should be(true)
    }

    "save alert only if absent" in {

      val email: Email = Email("test", EmailAddress("test@test.it"), "test", EmailAddress("test@test.it"))
      val jobID: String = UUID.randomUUID().toString
      val res = await(repo.saveAlertIfAbsent(EmailAlert(email = email, sent = None, persisted = Some(DateTime.now), jobId = jobID)))
      res.isDefined shouldBe true

      val res2 = await(repo.saveAlertIfAbsent(EmailAlert(email = email, sent = None, persisted = Some(DateTime.now), jobId = jobID)))
      res2.isDefined shouldBe false

    }

    "return jobs affected by tube delays" in new WithApplication() {

      await(repo.deleteAllJobs())

      def jobJson (hourOfTheDay: Int, minOfTheHour: Int) = s"""{
                  |  "accountId": "accountId",
                  |  "journey": {
                  |    "meansOfTransportation": {
                  |      "trainService": [],
                  |      "tubeLines": [
                  |        {
                  |          "name": "piccadilly",
                  |          "id": "piccadilly"
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
                  |      "value": "from@mss.it"
                  |    },
                  |    "nameFrom": "name",
                  |    "to": {
                  |      "value": "rocco_bruno@msn.com"
                  |    }
                  |  },
                  |  "docType": "Job",
                  |  "active": true,
                  |  "id": "06e0fb68-adb6-4c85-a8cd-923cdd00beaf",
                  |  "title": "jobTile"
                  |}""".stripMargin


      private val hourOfTheDay: Int = now().hourOfDay().get()
      private val minutesOfTheHour: Int = now().minuteOfHour().get()
      val job = Json.fromJson[Job](Json.parse(jobJson(hourOfTheDay, minutesOfTheHour))).get

        await(repo.save(job))


      val result = await(repo.findJobsByTubeLineAndRunningTime(Seq(TubeLine("piccadilly","piccadilly")), DateTime.now().withHourOfDay(hourOfTheDay).withMinuteOfHour(minutesOfTheHour)))

      result.size shouldBe 1
      result(0).getId shouldBe "06e0fb68-adb6-4c85-a8cd-923cdd00beaf"


    }


  }

  private def startTimeOfDay(nowTime: DateTime) = {
    val hourOfTheDay = nowTime.hourOfDay().get()
    val minOfTheDay = nowTime.minuteOfHour().get()
    TimeOfDay(hourOfTheDay, minOfTheDay, Some(TimeOfDay.time(hourOfTheDay, minOfTheDay)))
  }


}
