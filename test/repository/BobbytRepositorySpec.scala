package repository

import java.util.UUID
import javax.inject.Inject

import model._
import org.joda.time.DateTime
import org.joda.time.DateTime.now
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.libs.json.Json
import play.api.test.WithApplication
import util.Testing

import scala.concurrent.duration._
@RunWith(classOf[JUnitRunner])
class BobbytRepositorySpec  extends Testing {

  "a repository" should {

    "return valid token" in new WithApplication {

      await(bobbytRepository.deleteAllToken())
      private val token = Token(token = "token", accountId = Some("accountId"), userId = "userId")
      await(bobbytRepository.saveToken(token))

      val res = await(bobbytRepository.findValidTokenByValue("token"), 10 second)
      res.size should be(1)
      res contains token should be(true)
    }

    "save alert only if absent" in {

      val email: Email = Email("test", EmailAddress("test@test.it"), "test", EmailAddress("test@test.it"))
      val jobID: String = UUID.randomUUID().toString
      val res = await(bobbytRepository.saveAlertIfAbsent(EmailAlert(email = email, sentAt = None, persisted = DateTime.now, jobId = jobID, sent = true)))
      res.isDefined shouldBe true

      val res2 = await(bobbytRepository.saveAlertIfAbsent(EmailAlert(email = email, sentAt = None, persisted = DateTime.now, jobId = jobID)))
      res2.isDefined shouldBe false

    }

    "return jobs affected by tube delays" in new WithApplication() {

      await(bobbytRepository.deleteAllJobs())

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

        await(bobbytRepository.save(job))


      val result = await(bobbytRepository.findJobsByTubeLineAndRunningTime(Seq(TubeLine("piccadilly","piccadilly")), DateTime.now().withHourOfDay(hourOfTheDay).withMinuteOfHour(minutesOfTheHour)))

      result.size shouldBe 1
      result(0).getId shouldBe "06e0fb68-adb6-4c85-a8cd-923cdd00beaf"


    }

    "set an alert as sent" in {

      val email: Email = Email("test", EmailAddress("test@test.it"), "test", EmailAddress("test@test.it"))
      val jobID: String = UUID.randomUUID().toString
      val res = await(bobbytRepository.saveAlert(EmailAlert(email = email, sentAt = None, persisted = DateTime.now, jobId = jobID)))
      res.isDefined shouldBe true

      val notUpdatedAlert = await(bobbytRepository.findAlertByJobIdAndSentValue(jobID))
      notUpdatedAlert.isDefined shouldBe true
      notUpdatedAlert.get.sent shouldBe false

      await(bobbytRepository.markAlertAsSent(res.get))

      val updateAlert = await(bobbytRepository.findAlertByJobIdAndSentValue(jobID, sent = true))
      updateAlert.isDefined shouldBe true
      updateAlert.get.sent shouldBe true

    }

    "set an alert as sentAt" in {

      val email: Email = Email("test", EmailAddress("test@test.it"), "test", EmailAddress("test@test.it"))
      val jobID: String = UUID.randomUUID().toString
      val res = await(bobbytRepository.saveAlert(EmailAlert(email = email, sentAt = None, persisted = DateTime.now, jobId = jobID)))
      res.isDefined shouldBe true

      val notUpdatedAlert = await(bobbytRepository.findAlertByJobIdAndSentValue(jobID))
      notUpdatedAlert.isDefined shouldBe true
      notUpdatedAlert.get.sentAt.isDefined shouldBe false

      await(bobbytRepository.markAlertAsSentAt(res.get))

      val updateAlert = await(bobbytRepository.findAlertByJobIdAndSentValue(jobID))
      updateAlert.isDefined shouldBe true
      updateAlert.get.sentAt.isDefined shouldBe true

    }

    "return alert to delete" in {

      val email: Email = Email("test", EmailAddress("test@test.it"), "test", EmailAddress("test@test.it"))
      val jobID: String = UUID.randomUUID().toString
      val res = await(bobbytRepository.saveAlert(EmailAlert(email = email, sentAt = Some(DateTime.now().minusDays(2)), persisted = DateTime.now, jobId = jobID, sent = true)))
      val res2 = await(bobbytRepository.saveAlert(EmailAlert(email = email, sentAt = Some(DateTime.now()), persisted = DateTime.now, jobId = jobID, sent = true)))
      val res1 = await(bobbytRepository.saveAlert(EmailAlert(email = email, sentAt = Some(DateTime.now().minusDays(2)), persisted = DateTime.now, jobId = jobID, sent = false)))
      res.isDefined shouldBe true
      res2.isDefined shouldBe true
      res1.isDefined shouldBe true

      val results = await(bobbytRepository.findAllAlertSentYesterday())
      results.size shouldBe 1
      results(0) == res.get


      await(bobbytRepository.deleteById(res.get))
      await(bobbytRepository.deleteById(res2.get))
      await(bobbytRepository.deleteById(res1.get))

    }


  }

  private def startTimeOfDay(nowTime: DateTime) = {
    val hourOfTheDay = nowTime.hourOfDay().get()
    val minOfTheDay = nowTime.minuteOfHour().get()
    TimeOfDay(hourOfTheDay, minOfTheDay, Some(TimeOfDay.time(hourOfTheDay, minOfTheDay)))
  }


}
