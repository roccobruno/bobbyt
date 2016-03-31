package controller

import java.util.UUID

import model.{Jack}
import org.junit.runner.RunWith
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.libs.json.{Json, JsLookupResult, JsValue}
import play.api.test.{WithApplication, FakeRequest}
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class JackControllerSpec extends Specification {

  "Bobby controller" should {

    val id = "12345"


    "return 200 when posting a Bobby record" in new WithApplication {

      private val id = UUID.randomUUID().toString
      private val bobby = Jack(firstName = "test", lastName = "testr")
      val response = route(FakeRequest(POST, "/api/bobby").withBody(Json.toJson(bobby)))
      status(response.get) must equalTo(CREATED)

      val getResource = headers(response.get).get("Location").get
      getResource must be startWith("/api/bobby")

      val getRec = route(FakeRequest(GET, getResource)).get

      status(getRec) must equalTo(OK)
      val json: Jack = contentAsJson(getRec).as[Jack]
      json.firstName must equalTo("test")
      json.lastName must equalTo("testr")

      val delResponse = route(FakeRequest(DELETE, getResource))
      status(delResponse.get) must equalTo(OK)

      val getRecR = route(FakeRequest(GET, getResource)).get
      status(getRecR) must equalTo(NOT_FOUND)

    }



  }

}
