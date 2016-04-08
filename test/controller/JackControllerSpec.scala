package controller

import java.util.UUID

import model._
import org.joda.time.LocalDate
import org.junit.runner.RunWith
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.libs.json.{Json, JsLookupResult, JsValue}
import play.api.test.{WithApplication, FakeRequest}
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class JackControllerSpec extends Specification {

  "jack controller" should {

    val id = "12345"


    "return 201 when posting a jack record" in new WithApplication {

      private val id = UUID.randomUUID().toString
      private val job = Job(alert = Email("from@mss.it","from@mss.it"),journey= Journey(true,MeansOfTransportation(Seq(TubeLine("central","central")),Nil),TimeOfDay(8,30),40))
      val response = route(implicitApp,FakeRequest(POST, "/api/jack").withBody(Json.toJson(job)))
      status(response.get) must equalTo(CREATED)

      val getResource = headers(response.get).get("Location").get
      getResource must be startWith("/api/jack")

      val getRec = route(implicitApp,FakeRequest(GET, getResource)).get

      status(getRec) must equalTo(OK)
      val json: Job = contentAsJson(getRec).as[Job]
      json.alert.from must equalTo("from@mss.it")
      json.alert.to must equalTo("from@mss.it")

      val delResponse = route(implicitApp,FakeRequest(DELETE, getResource))
      status(delResponse.get) must equalTo(OK)

      val getRecR = route(implicitApp,FakeRequest(GET, getResource)).get
      status(getRecR) must equalTo(NOT_FOUND)

    }

    "return 201 and create a running job with a job creation" in new WithApplication {
      private val id = UUID.randomUUID().toString
      private val timeOfDay = TimeOfDay(8, 30)
      private val job = Job(alert = Email("from@mss.it","from@mss.it"),journey= Journey(true,MeansOfTransportation(Seq(TubeLine("central","central")),Nil),timeOfDay,40))
      val response = route(implicitApp,FakeRequest(POST, "/api/jack").withBody(Json.toJson(job)))
      status(response.get) must equalTo(CREATED)

      val getResource = headers(response.get).get("Location").get
      getResource must be startWith("/api/jack")

      val getRec = route(implicitApp,FakeRequest(GET, getResource)).get
      status(getRec) must equalTo(OK)
      val json: Job = contentAsJson(getRec).as[Job]
      val jobId = json.getId

      val delResponse = route(implicitApp,FakeRequest(DELETE, getResource))
      status(delResponse.get) must equalTo(OK)

      val runningJobResp = route(implicitApp,FakeRequest(GET,s"/api/jack/running-job/job-id/$jobId")).get
      status(runningJobResp) must equalTo(OK)
      val runningJobJson: RunningJob = contentAsJson(runningJobResp).as[RunningJob]

      runningJobJson.from must equalTo(timeOfDay)
      runningJobJson.to must equalTo(TimeOfDay(9,10))

      val delRunningJobResponse = route(implicitApp,FakeRequest(DELETE, s"/api/jack/running-job/id/${runningJobJson.getId}"))
      status(delRunningJobResponse.get) must equalTo(OK)
    }



  }

}
