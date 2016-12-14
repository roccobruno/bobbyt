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

    "return running job to execute" in new WithApplication {

      await(repo.deleteAllRunningJob(), 10 second)
      await(repo.deleteAllJobs(), 10 second)

      val now = DateTime.now.plusMinutes(2)
      val hourOfTheDay = now.hourOfDay().get()
      val minOfTheDay = now.minuteOfHour().get()
      val startJob = TimeOfDay(hourOfTheDay, minOfTheDay, Some(TimeOfDay.time(hourOfTheDay, minOfTheDay)))

      val job = RunningJob(fromTime = startJob, toTime = startJob.plusMinutes(40), alertSent = false,
        recurring = true, jobId = UUID.randomUUID().toString)
      await(repo.saveRunningJob(job))
      await(repo.saveRunningJob(RunningJob(fromTime = startJob, toTime = startJob.plusMinutes(40), alertSent = false,
        recurring = true, jobId = UUID.randomUUID().toString)))

      val job1 = RunningJob(fromTime = startJob, toTime = startJob.plusMinutes(50), alertSent = true,
        recurring = true, jobId = UUID.randomUUID().toString)
      await(repo.saveRunningJob(job1))


      val res = await(repo.findRunningJobToExecute(), 10 second)
      res.size should be(2)

      res contains job should be(true)

    }


    "return running job to execute with end time" in new WithApplication {

      await(repo.deleteAllRunningJob(), 10 second)
      await(repo.deleteAllJobs(), 10 second)

      val now = DateTime.now.minusMinutes(10)
      val hourOfTheDay = now.hourOfDay().get()
      val minOfTheDay = now.minuteOfHour().get()
      val startJob = TimeOfDay(hourOfTheDay, minOfTheDay, Some(TimeOfDay.time(hourOfTheDay, minOfTheDay)))

      val job = RunningJob(fromTime = startJob, toTime = startJob.plusMinutes(40), alertSent = false,
        recurring = true, jobId = UUID.randomUUID().toString)
      await(repo.saveRunningJob(job))

      await(repo.saveRunningJob(RunningJob(fromTime = startJob, toTime = startJob.plusMinutes(40), alertSent = false,
        recurring = true, jobId = UUID.randomUUID().toString)))

      val job1 = RunningJob(fromTime = startJob, toTime = startJob.plusMinutes(50), alertSent = true,
        recurring = true, jobId = UUID.randomUUID().toString)
      await(repo.saveRunningJob(job1))


      val res = await(repo.findRunningJobToExecute(), 10 second)
      res.size should be(2)

      res contains job should be(true)

    }


    "return running jobs to reset" in new WithApplication {

      await(repo.deleteAllRunningJob(), 10 second)

      val startJob = startTimeOfDay(now.minusHours(2))
      val jId = UUID.randomUUID().toString
      val job = RunningJob(fromTime = startJob, toTime = startJob.plusMinutes(40), alertSent = true,
        recurring = true, jobId = jId)
      await(repo.saveRunningJob(job))

      val job1 = RunningJob(fromTime = startJob, toTime = startJob.plusMinutes(50), alertSent = false,
        recurring = true, jobId = UUID.randomUUID().toString)
      await(repo.saveRunningJob(job1))

      val startTimeOfDay1 = startTimeOfDay(now.plusMinutes(2))
      val job2 = RunningJob(fromTime = startTimeOfDay1, toTime = startTimeOfDay1.plusMinutes(50), alertSent = true,
        recurring = true, jobId = UUID.randomUUID().toString)
      await(repo.saveRunningJob(job2))


      val res = await(repo.findRunningJobToReset(), 10 second)
      res.size should be(1)
      res contains job2 should be(true)

    }

    "return valid token" in new WithApplication {

      await(repo.deleteAllToken())
      private val token = Token(token = "token", accountId = "accountId")
      await(repo.saveToken(token))

      val res = await(repo.findValidTokenByValue("token"), 10 second)
      res.size should be(1)
      res contains token should be(true)
    }

    "return jobs affected by tube delays" in new WithApplication() {

      await(repo.deleteAllJobs())

      val jobJson = """{
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
                  |      "hour": 17,
                  |      "min": 27,
                  |      "time": 1727
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
                  |      "value": "from@mss.it"
                  |    }
                  |  },
                  |  "docType": "Job",
                  |  "active": true,
                  |  "id": "06e0fb68-adb6-4c85-a8cd-923cdd00beaf",
                  |  "title": "jobTile"
                  |}""".stripMargin

       val job = Json.fromJson[Job](Json.parse(jobJson)).get

        await(repo.save(job))


      val result = await(repo.findJobsByTubeLineAndRunningTime(Seq(TubeLine("piccadilly","piccadilly")), DateTime.now().withHourOfDay(17).withMinuteOfHour(30)))

      result.size shouldBe 1
      result(0).id shouldBe "06e0fb68-adb6-4c85-a8cd-923cdd00beaf"


    }


  }

  private def startTimeOfDay(nowTime: DateTime) = {
    val hourOfTheDay = nowTime.hourOfDay().get()
    val minOfTheDay = nowTime.minuteOfHour().get()
    TimeOfDay(hourOfTheDay, minOfTheDay, Some(TimeOfDay.time(hourOfTheDay, minOfTheDay)))
  }


}
