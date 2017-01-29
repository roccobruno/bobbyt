package controller

import java.util.UUID
import javax.inject.Inject

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
import repository.{BobbytRepository, ClusterConfiguration}
import util.{Testing, TokenUtil}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._


@RunWith(classOf[JUnitRunner])
class BobbytControllerSpec extends Specification  {

  case class TestJob(job: Job, location: String)

  lazy  val appEnableSec = GuiceApplicationBuilder().loadConfig(Configuration("security-enabled" -> true)).build()

  trait Setup extends WithApplication with TokenUtil  {

    lazy val id = UUID.randomUUID().toString
    def jobDuration = 40
    lazy val journey: Journey = Journey(true, MeansOfTransportation(Seq(TubeLine("central", "central")), Nil), TimeOfDay(8, 30), 40)
    lazy val job = Job("jobTitle",alert = Email("name",EmailAddress("from@mss.it"),"name",EmailAddress("from@mss.it")),
      journey= Journey(true,MeansOfTransportation(Seq(TubeLine("central","central")),Nil),TimeOfDay(8,30),jobDuration),accountId = "accountId")
    lazy val toJson = Json.toJson(job)


    lazy val bobbytRepos = new BobbytRepository(new ClusterConfiguration(app.configuration))

    def cleanUpDBAndCreateToken = {
      Await.result( for {
        del <-bobbytRepos.deleteAllToken()
        del <-bobbytRepos.deleteAllAccount()
        del <-bobbytRepos.deleteAllAlerts()
        del <-bobbytRepos.deleteAllJobs()
      } yield del, 10 seconds)

    }

    def postANewJob = {
      val response = route(implicitApp, FakeRequest(POST, "/api/bobbyt").withBody(toJson).withHeaders((HeaderNames.AUTHORIZATION,
        s"Bearer $token")))
      status(response.get) must equalTo(CREATED)
      val location = headers(response.get).get("Location").get

      location must be startWith "/api/bobbyt"

      val getRec = route(implicitApp, FakeRequest(GET, location).withHeaders((HeaderNames.AUTHORIZATION,
        s"Bearer $token"))).get

      status(getRec) must equalTo(OK)
      val json: Job = contentAsJson(getRec).as[Job]
      json.alert.from must equalTo(EmailAddress("from@mss.it"))
      json.alert.to must equalTo(EmailAddress("from@mss.it"))
      TestJob(json, location)
    }

    def login = {
      val resp = route(implicitApp, FakeRequest(POST, "/api/bobbyt/login-token").withHeaders((HeaderNames.AUTHORIZATION,
        s"Bearer $token")).withJsonBody(Json.parse("{}"))).get
      status(resp) must equalTo(CREATED)
    }

  }



  "bobbyt controller" should {

    val id = "12345"


    "return 200 when starting jobs through fetch Line end point" in new Setup{

      val allJob = route(implicitApp,FakeRequest(GET, "/api/bobbyt/fetch")).get
      status(allJob) must equalTo(OK)
    }


    "return 201 when posting a bobbyt record" in new Setup {

      cleanUpDBAndCreateToken

      login

      postANewJob

      //look up job by token
      val allJob = route(implicitApp,FakeRequest(GET, "/api/bobbyt/job/all").withHeaders((HeaderNames.AUTHORIZATION,
        s"Bearer $token"))).get
      status(allJob) must equalTo(OK)
      val jobs: Seq[Job] = contentAsJson(allJob).as[Seq[Job]]
      jobs.size must equalTo(1)



    }


    "return 400 when updating a bobbyt job that lasts more than 1 hour" in new Setup {


      override def jobDuration = 300
      cleanUpDBAndCreateToken

      login

      //create a job
      val response = route(implicitApp, FakeRequest(POST, "/api/bobbyt").withBody(Json.toJson(job)).withHeaders((HeaderNames.AUTHORIZATION,
        s"Bearer $token"))).get
      status(response) must equalTo(BAD_REQUEST)

      (contentAsJson(response) \ "errorCode").as[Int] must equalTo(4001)
      (contentAsJson(response) \ "message").as[String] must equalTo("Job with wrong duration. It cannot be longer than 120 mins")

    }

    "return 400 when updating a 4th bobbyt job for the same account" in new Setup {

      cleanUpDBAndCreateToken

      login

      //create a job
      postANewJob
      postANewJob
      postANewJob

      val response = route(implicitApp, FakeRequest(POST, "/api/bobbyt").withBody(Json.toJson(job)).withHeaders((HeaderNames.AUTHORIZATION,
        s"Bearer $token"))).get
      status(response) must equalTo(BAD_REQUEST)

      (contentAsJson(response) \ "errorCode").as[Int] must equalTo(4002)
      (contentAsJson(response) \ "message").as[String] must equalTo("Too many jobs created for the same account. Limit is 3")

    }



    "return 200 when updating a bobbyt job" in new Setup {

      cleanUpDBAndCreateToken

      login

      //create a job
      val jobCreated = postANewJob



      //updating the same job
      val updatedJob = jobCreated.job.copy(title = "Updated Job", journey = journey.copy(meansOfTransportation =
        MeansOfTransportation(Seq(TubeLine("central", "central"), TubeLine("piccadilly", "piccadilly")), Nil)))
      val responseForUpdate = route(implicitApp, FakeRequest(PUT, "/api/bobbyt").withBody(Json.toJson(updatedJob)).withHeaders((HeaderNames.AUTHORIZATION,
        s"Bearer $token")))
      status(responseForUpdate.get) must equalTo(OK)

      val getRecUpdated = route(implicitApp, FakeRequest(GET, jobCreated.location).withHeaders((HeaderNames.AUTHORIZATION,
        s"Bearer $token"))).get

      status(getRecUpdated) must equalTo(OK)
      val updatedJobGet: Job = contentAsJson(getRecUpdated).as[Job]
      updatedJobGet.title must equalTo("Updated Job")
      updatedJobGet.journey.meansOfTransportation.tubeLines.size must equalTo(2)

    }

    "return 200 when deleting a bobbyt job" in new Setup {

      cleanUpDBAndCreateToken

      login



      //create a job
      val jobCreated = postANewJob


      //deleting the job

      val deleteResp  = route(implicitApp, FakeRequest(DELETE, jobCreated.location).withHeaders((HeaderNames.AUTHORIZATION,
        s"Bearer $token"))).get
      status(deleteResp) must equalTo(OK)

      // the GET should return 404 now
      val getRecAfterDEeting = route(implicitApp, FakeRequest(GET, jobCreated.location).withHeaders((HeaderNames.AUTHORIZATION,
        s"Bearer $token"))).get

      status(getRecAfterDEeting) must equalTo(NOT_FOUND)

    }






  }

}
