package controller

import java.util.UUID

import model._
import org.joda.time.{DateTime, LocalDate}
import org.junit.runner.RunWith
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{Json, JsLookupResult, JsValue}
import play.api.mvc.Cookies
import play.api.test.{WithApplication, FakeRequest}
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class BobbitControllerSpec extends Specification {

  lazy  val appEnableSec = GuiceApplicationBuilder().loadConfig(Configuration("security-enabled" -> true)).build()


  "bobbit controller" should {

    val id = "12345"


    "return 201 when posting a bobbit record" in new WithApplication {

      private val id = UUID.randomUUID().toString
      private val job = Job("jobTitle",alert = Email("name",EmailAddress("from@mss.it"),"name",EmailAddress("from@mss.it")),journey= Journey(true,MeansOfTransportation(Seq(TubeLine("central","central")),Nil),TimeOfDay(8,30),40))
      private val toJson = Json.toJson(job)

      val response = route(implicitApp,FakeRequest(POST, "/api/bobbit").withBody(toJson))
      status(response.get) must equalTo(CREATED)

      val getResource = headers(response.get).get("Location").get
      getResource must be startWith "/api/bobbit"

      val getRec = route(implicitApp,FakeRequest(GET, getResource)).get

      status(getRec) must equalTo(OK)
      val json: Job = contentAsJson(getRec).as[Job]
      json.alert.from must equalTo(EmailAddress("from@mss.it"))
      json.alert.to must equalTo(EmailAddress("from@mss.it"))

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
      private val jobStartsAtTime = TimeOfDay(hourOfTheDay, minOfTheDay, Some(TimeOfDay.time(hourOfTheDay, minOfTheDay)))
      private val job = Job("jobTile",alert = Email("name",EmailAddress("from@mss.it"),"name",EmailAddress("from@mss.it")),journey=
        Journey(true,MeansOfTransportation(Seq(TubeLine("northern","northern")),Nil),jobStartsAtTime,40))
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

      status(route(implicitApp,FakeRequest(GET,s"/api/bobbit/running-job/job-id/${runningJobJson.jobId}")).get) must equalTo(OK)

      val activeRunningJobResp = route(implicitApp,FakeRequest(GET,s"/api/bobbit/running-job/active")).get
      status(runningJobResp) must equalTo(OK)
      contentAsJson(activeRunningJobResp).as[Seq[RunningJob]].size must equalTo(1)

      val delRunningJobResponse = route(implicitApp,FakeRequest(DELETE, s"/api/bobbit/running-job/id/${runningJobJson.getId}"))
      status(delRunningJobResponse.get) must equalTo(OK)

      val delResponse = route(implicitApp,FakeRequest(DELETE, getResource))
      status(delResponse.get) must equalTo(OK)


    }

    "return 201 and create account record" in new WithApplication() {

      val account = Account(userName = "neo13",firstName = Some("Rocco"),lastName = Some("Bruno"), email = EmailAddress("test@test.it"),password ="passw")
      val response = route(implicitApp,FakeRequest(POST, "/api/bobbit/account").withBody(Json.toJson(account)))
      status(response.get) must equalTo(CREATED)

      val getResource = headers(response.get).get("Location").get
      getResource must be startWith("/api/bobbit/account")

      val getRec = route(implicitApp,FakeRequest(GET, getResource)).get
      status(getRec) must equalTo(OK)
      val json: Account = contentAsJson(getRec).as[Account]

      json.userName must equalTo(account.userName)
      json.firstName must equalTo(account.firstName)
      json.lastName must equalTo(account.lastName)
      json.email must equalTo(account.email)
    }

    "create and validate account" in new WithApplication() {

      val username: String = "neo13"
      val passw: String = "passw"
      val account = Account(userName = username,firstName = Some("Rocco"),lastName = Some("Bruno"), email = EmailAddress("test@test.it"), password =passw)
      val response = route(implicitApp,FakeRequest(POST, "/api/bobbit/account").withBody(Json.toJson(account)))
      status(response.get) must equalTo(CREATED)

      val getResource = headers(response.get).get("Location").get
      getResource must be startWith("/api/bobbit/account")

      val getRec = route(implicitApp,FakeRequest(GET, getResource)).get
      status(getRec) must equalTo(OK)
      val json: Account = contentAsJson(getRec).as[Account]

      json.active must equalTo(false)

      val token = headers(response.get).get(AUTHORIZATION).get
      val valRespo = route(implicitApp,FakeRequest(POST, s"/api/bobbit/account/validate/$token").withBody(Json.toJson("")))
      status(valRespo.get) must equalTo(OK)


      val getRecUpdated = route(implicitApp,FakeRequest(GET, getResource)).get
      status(getRecUpdated) must equalTo(OK)
      val jsonUpdated: Account = contentAsJson(getRecUpdated).as[Account]

      jsonUpdated.active must equalTo(true)
      
      //login
      val loginRespo = route(implicitApp,FakeRequest(POST, s"/api/bobbit/login").withBody(Json.toJson(Login(username = username,password = passw)))).get
      status(loginRespo) must equalTo(OK)

      val cookiesReturned: Cookies = cookies(loginRespo)
      val tokenAuth = cookiesReturned.find( c => c.name =="token").get.value
      println(s"token = $tokenAuth")


      val tokenValidate = route(implicitApp,FakeRequest(POST, s"/api/bobbit/token/validate/$tokenAuth").withBody(Json.toJson(""))).get
      status(tokenValidate) must equalTo(OK)

    }








  }

}
