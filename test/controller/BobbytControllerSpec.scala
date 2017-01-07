package controller

import java.util.UUID

import model._
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.Configuration
import play.api.http.HeaderNames
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeRequest, WithApplication}
import repository.BobbytRepository
import util.TokenUtil

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._


@RunWith(classOf[JUnitRunner])
class BobbytControllerSpec extends Specification  {

  lazy  val appEnableSec = GuiceApplicationBuilder().loadConfig(Configuration("security-enabled" -> true)).build()

  trait Setup extends WithApplication with TokenUtil {
    val bobbytRepos = BobbytRepository

    def cleanUpDBAndCreateToken = {
      Await.result( for {
        del <-bobbytRepos.deleteAllToken()
        del <-bobbytRepos.deleteAllAccount()
        del <-bobbytRepos.deleteAllAlerts()
        del <-bobbytRepos.deleteAllJobs()
      } yield del, 10 seconds)

    }

  }



  "bobbyt controller" should {

    val id = "12345"


    "return 201 when posting a bobbyt record" in new Setup {

      cleanUpDBAndCreateToken

      private val id = UUID.randomUUID().toString
      private val job = Job("jobTitle",alert = Email("name",EmailAddress("from@mss.it"),"name",EmailAddress("from@mss.it")),
        journey= Journey(true,MeansOfTransportation(Seq(TubeLine("central","central")),Nil),TimeOfDay(8,30),40),accountId = "accountId")
      private val toJson = Json.toJson(job)

      val response = route(implicitApp, FakeRequest(POST, "/api/bobbyt").withBody(toJson).withHeaders((HeaderNames.AUTHORIZATION,
        s"Bearer $token")))
      status(response.get) must equalTo(CREATED)

      val getResource = headers(response.get).get("Location").get
      getResource must be startWith "/api/bobbyt"

      val getRec = route(implicitApp, FakeRequest(GET, getResource).withHeaders((HeaderNames.AUTHORIZATION,
        s"Bearer $token"))).get

      status(getRec) must equalTo(OK)
      val json: Job = contentAsJson(getRec).as[Job]
      json.alert.from must equalTo(EmailAddress("from@mss.it"))
      json.alert.to must equalTo(EmailAddress("from@mss.it"))

      //look up job by token
      val allJob = route(implicitApp,FakeRequest(GET, "/api/bobbyt/job/all").withHeaders((HeaderNames.AUTHORIZATION,
        s"Bearer $token"))).get
      status(allJob) must equalTo(OK)
      val jobs: Seq[Job] = contentAsJson(allJob).as[Seq[Job]]
      jobs.size must equalTo(1)



    }



    "return 201 and create account record" in new Setup() {
      cleanUpDBAndCreateToken
      val account = Account(userName = "neo13",email = Some(EmailAddress("test@test.it")), psw = Some("passw"))
      private val toJson = Json.toJson(account)

      val response = route(implicitApp,FakeRequest(POST, "/api/bobbyt/account").withBody(Json.parse("""{"userName":"neo13","email":{"value":"test@test.it"},"psw":"passw","active":false, "docType":"Account"}""")))
      status(response.get) must equalTo(CREATED)

      val getResource = headers(response.get).get("Location").get
      getResource must be startWith("/api/bobbyt/account")

      val getRec = route(implicitApp,FakeRequest(GET, getResource).withHeaders((HeaderNames.AUTHORIZATION,
        s"Bearer $token"))).get
      status(getRec) must equalTo(OK)
      val json: Account = contentAsJson(getRec).as[Account]

      json.userName must equalTo(account.userName)
      json.firstName must equalTo(account.firstName)
      json.lastName must equalTo(account.lastName)
      json.email must equalTo(account.email)

    }

    "create and validate account" in new Setup() {

      cleanUpDBAndCreateToken

      val username: String = "neo13"
      val passw: String = "passw"
      val account = Account(userName = username,firstName = Some("Rocco"),lastName = Some("Bruno"), email = Some(EmailAddress("test@test.it")),
        psw = Some(passw))
      val response = route(implicitApp,FakeRequest(POST, "/api/bobbyt/account").withBody(Json.toJson(account)))
      status(response.get) must equalTo(CREATED)

      val getResource = headers(response.get).get("Location").get
      getResource must be startWith("/api/bobbyt/account")


      val ttoken = headers(response.get).get(AUTHORIZATION).get
      val valRespo = route(implicitApp,FakeRequest(POST, s"/api/bobbyt/account/validate").withHeaders((HeaderNames.AUTHORIZATION,
        ttoken)))
      status(valRespo.get) must equalTo(303)
      val redirectAfterValidation = headers(valRespo.get).get("Location").get
      redirectAfterValidation must equalTo("/accountactive")




      val getRecUpdated = route(implicitApp,FakeRequest(GET, getResource).withHeaders((HeaderNames.AUTHORIZATION,
        ttoken))).get
      status(getRecUpdated) must equalTo(OK)
      val jsonUpdated: Account = contentAsJson(getRecUpdated).as[Account]

      jsonUpdated.active must equalTo(true)

      //lookup account by token
      val getRecByToken = route(implicitApp,FakeRequest(GET, "/api/bobbyt/account/load").withHeaders((HeaderNames.AUTHORIZATION,
        ttoken))).get
      status(getRecByToken) must equalTo(OK)


    }


    //TODO LOGOUT
//    "logout a valid account" in new Setup {
//
//      val response = route(implicitApp,FakeRequest(POST, "/api/bobbyt/logout").withBody("").withHeaders((HeaderNames.AUTHORIZATION,
//        s"Bearer $token"))).get
//      status(response) must equalTo(OK)
//
//      val runningJobResp = route(implicitApp,FakeRequest(GET,s"/api/bobbyt/account/load")
//        .withHeaders((HeaderNames.AUTHORIZATION,s"Bearer $token"))).get
//      status(runningJobResp) must equalTo(FORBIDDEN)
//
//    }





  }

}
