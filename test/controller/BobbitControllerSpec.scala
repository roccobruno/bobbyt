package controller

import java.util.UUID

import model._
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.Configuration
import play.api.http.HeaderNames
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Cookies
import play.api.test.Helpers._
import play.api.test.{FakeRequest, WithApplication}
import repository.BobbitRepository
import service.BearerTokenGenerator

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
@RunWith(classOf[JUnitRunner])
class BobbitControllerSpec extends Specification  {

  lazy  val appEnableSec = GuiceApplicationBuilder().loadConfig(Configuration("security-enabled" -> true)).build()

  trait Setup extends WithApplication {
    val bobbitRepos = BobbitRepository
    val tokenForSecurity = Token(token = BearerTokenGenerator.generateSHAToken("account-token"), accountId = Some("accountId"), userId = "userId")
    def saveToken = {
      bobbitRepos.saveToken(tokenForSecurity)
    }

    def cleanUpDBAndCreateToken = {
      Await.result( for {
        del <-bobbitRepos.deleteAllToken()
        del <-bobbitRepos.deleteAllAccount()
        del <-bobbitRepos.deleteAllAlerts()
        del <-bobbitRepos.deleteAllJobs()
        tok <- saveToken
      } yield del, 10 seconds)

    }

  }



  "bobbit controller" should {

    val id = "12345"


    "return 201 when posting a bobbit record" in new Setup {

      cleanUpDBAndCreateToken

      private val id = UUID.randomUUID().toString
      private val job = Job("jobTitle",alert = Email("name",EmailAddress("from@mss.it"),"name",EmailAddress("from@mss.it")),
        journey= Journey(true,MeansOfTransportation(Seq(TubeLine("central","central")),Nil),TimeOfDay(8,30),40),accountId = "accountId")
      private val toJson = Json.toJson(job)

      val response = route(implicitApp,FakeRequest(POST, "/api/bobbit").withBody(toJson).withHeaders((HeaderNames.AUTHORIZATION,
        tokenForSecurity.token)))
      status(response.get) must equalTo(CREATED)

      val getResource = headers(response.get).get("Location").get
      getResource must be startWith "/api/bobbit"

      val getRec = route(implicitApp,FakeRequest(GET, getResource).withHeaders((HeaderNames.AUTHORIZATION,
        tokenForSecurity.token))).get

      status(getRec) must equalTo(OK)
      val json: Job = contentAsJson(getRec).as[Job]
      json.alert.from must equalTo(EmailAddress("from@mss.it"))
      json.alert.to must equalTo(EmailAddress("from@mss.it"))

      //look up job by token
      val allJob = route(implicitApp,FakeRequest(GET, "/api/bobbit/job/all").withHeaders((HeaderNames.AUTHORIZATION,
        tokenForSecurity.token))).get
      status(allJob) must equalTo(OK)
      val jobs: Seq[Job] = contentAsJson(allJob).as[Seq[Job]]
      jobs.size must equalTo(1)



    }



    "return 201 and create account record" in new Setup() {
      cleanUpDBAndCreateToken
      val account = Account(userName = "neo13",email = Some(EmailAddress("test@test.it")), psw = Some("passw"))
      private val toJson = Json.toJson(account)

      val response = route(implicitApp,FakeRequest(POST, "/api/bobbit/account").withBody(Json.parse("""{"userName":"neo13","email":{"value":"test@test.it"},"psw":"passw","active":false, "docType":"Account"}""")))
      status(response.get) must equalTo(CREATED)

      val getResource = headers(response.get).get("Location").get
      getResource must be startWith("/api/bobbit/account")

      val getRec = route(implicitApp,FakeRequest(GET, getResource).withHeaders((HeaderNames.AUTHORIZATION,
        tokenForSecurity.token))).get
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
      val response = route(implicitApp,FakeRequest(POST, "/api/bobbit/account").withBody(Json.toJson(account)))
      status(response.get) must equalTo(CREATED)

      val getResource = headers(response.get).get("Location").get
      getResource must be startWith("/api/bobbit/account")


      val token = headers(response.get).get(AUTHORIZATION).get
      val valRespo = route(implicitApp,FakeRequest(GET, s"/api/bobbit/account/validate/$token"))
      status(valRespo.get) must equalTo(303)
      val redirectAfterValidation = headers(valRespo.get).get("Location").get
      redirectAfterValidation must equalTo("/accountactive")



      //login
      val loginRespo = route(implicitApp,FakeRequest(POST, s"/api/bobbit/login").withBody(Json.toJson(Login(username = username,password = passw))).withHeaders((HeaderNames.AUTHORIZATION,
        tokenForSecurity.token))).get
      status(loginRespo) must equalTo(OK)

      val cookiesReturned: Cookies = cookies(loginRespo)
      val tokenAuth = cookiesReturned.find( c => c.name =="token").get.value

      val getRecUpdated = route(implicitApp,FakeRequest(GET, getResource).withHeaders((HeaderNames.AUTHORIZATION,
        tokenAuth))).get
      status(getRecUpdated) must equalTo(OK)
      val jsonUpdated: Account = contentAsJson(getRecUpdated).as[Account]

      jsonUpdated.active must equalTo(true)

      val tokenValidate = route(implicitApp,FakeRequest(GET, s"/api/bobbit/token/validate/$tokenAuth")).get
      status(tokenValidate) must equalTo(OK)

      //lookup account by token
      val getRecByToken = route(implicitApp,FakeRequest(GET, "/api/bobbit/account/load").withHeaders((HeaderNames.AUTHORIZATION,
        tokenAuth))).get
      status(getRecByToken) must equalTo(OK)


    }


    "logout a valid account" in new Setup {

      val response = route(implicitApp,FakeRequest(POST, "/api/bobbit/logout").withBody("").withHeaders((HeaderNames.AUTHORIZATION,
        tokenForSecurity.token))).get
      status(response) must equalTo(OK)

      val runningJobResp = route(implicitApp,FakeRequest(GET,s"/api/bobbit/account/load")
        .withHeaders((HeaderNames.AUTHORIZATION,tokenForSecurity.token))).get
      status(runningJobResp) must equalTo(FORBIDDEN)

    }





  }

}
