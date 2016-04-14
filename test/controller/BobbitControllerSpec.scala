package controller

import java.util.UUID

import model._
import org.joda.time.{DateTime, LocalDate}
import org.junit.runner.RunWith
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.libs.json.{Json, JsLookupResult, JsValue}
import play.api.test.{WithApplication, FakeRequest}
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class BobbitControllerSpec extends Specification {

  "bobbit controller" should {

    val id = "12345"


    "return 201 when posting a bobbit record" in new WithApplication {

      private val id = UUID.randomUUID().toString
      private val job = Job(alert = Email(EmailAddress("rocco_bruno@msn.com"), EmailAddress("rocco_bruno@msn.com")),journey= Journey(true,MeansOfTransportation(Seq(TubeLine("central","central")),Nil),TimeOfDay(8,30),40))
      val response = route(implicitApp,FakeRequest(POST, "/api/bobbit").withBody(Json.toJson(job)))
      status(response.get) must equalTo(CREATED)

      val getResource = headers(response.get).get("Location").get
      getResource must be startWith("/api/bobbit")

      val getRec = route(implicitApp,FakeRequest(GET, getResource)).get

      status(getRec) must equalTo(OK)
      val json: Job = contentAsJson(getRec).as[Job]
      json.alert.from.value must equalTo("rocco_bruno@msn.com")
      json.alert.to.value must equalTo("rocco_bruno@msn.com")

      val delResponse = route(implicitApp,FakeRequest(DELETE, getResource))
      status(delResponse.get) must equalTo(OK)

      val getRecR = route(implicitApp,FakeRequest(GET, getResource)).get
      status(getRecR) must equalTo(NOT_FOUND)

    }

    "return 201 and create a running job with a job creation" in new WithApplication {
      private val id = UUID.randomUUID().toString

      private val now = DateTime.now.plusMinutes(2)
      private val hourOfTheDay = now.hourOfDay().get()
      private val minOfTheDay = now.minuteOfHour().get()
      private val jobStartsAtTime = TimeOfDay(hourOfTheDay, minOfTheDay, TimeOfDay.time(hourOfTheDay, minOfTheDay))
      private val job = Job(alert = Email(EmailAddress("rocco_bruno@test.com"), EmailAddress("rocco_bruno@msn.com")),journey=
        Journey(true,MeansOfTransportation(Seq(TubeLine("central","central")),Nil),jobStartsAtTime,40))
      val response = route(implicitApp,FakeRequest(POST, "/api/bobbit").withBody(Json.toJson(job)))
      status(response.get) must equalTo(CREATED)

      val getResource = headers(response.get).get("Location").get
      getResource must be startWith("/api/bobbit")

      val getRec = route(implicitApp,FakeRequest(GET, getResource)).get
      status(getRec) must equalTo(OK)
      val json: Job = contentAsJson(getRec).as[Job]
      val jobId = json.getId


      val runningJobResp = route(implicitApp,FakeRequest(GET,s"/api/bobbit/running-job/job-id/$jobId")).get
      status(runningJobResp) must equalTo(OK)
      val runningJobJson: RunningJob = contentAsJson(runningJobResp).as[RunningJob]

      runningJobJson.from must equalTo(jobStartsAtTime)

//      val activeRunningJobResp = route(implicitApp,FakeRequest(GET,s"/api/bobbit/running-job/active")).get
//      status(runningJobResp) must equalTo(OK)
//      contentAsJson(activeRunningJobResp).as[Seq[RunningJob]].size must equalTo(1)
//
//      val delRunningJobResponse = route(implicitApp,FakeRequest(DELETE, s"/api/bobbit/running-job/id/${runningJobJson.getId}"))
//      status(delRunningJobResponse.get) must equalTo(OK)
//
//      val delResponse = route(implicitApp,FakeRequest(DELETE, getResource))
//      status(delResponse.get) must equalTo(OK)


    }






  }

}
