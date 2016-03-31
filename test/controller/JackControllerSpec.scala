package controller

import java.util.UUID

import model.{Jack}
import org.junit.runner.RunWith
import org.scalatest.WordSpecLike
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.libs.json.{Json, JsLookupResult, JsValue}
import play.api.test.{WithApplication, FakeRequest}
import play.api.test.Helpers._

class JackControllerSpec extends WordSpecLike with org.scalatest.Matchers with org.scalatest.OptionValues {

  "Jack controller" should {

    val id = "12345"


    "return 200 when posting a Bobby record" in new WithApplication {

      private val id = UUID.randomUUID().toString
      private val bobby = Jack(firstName = "test", lastName = "testr")
      val response = route(FakeRequest(POST, "/api/bobby").withBody(Json.toJson(bobby)))
      status(response.get) should be (CREATED)

      val getResource = headers(response.get).get("Location").get
      getResource should  startWith("/api/bobby")

      val getRec = route(FakeRequest(GET, getResource)).get

      status(getRec) should be (OK)
      val json: Jack = contentAsJson(getRec).as[Jack]
      json.firstName should be ("test")
      json.lastName should be ("testr")

      val delResponse = route(FakeRequest(DELETE, getResource))
      status(delResponse.get) should be (OK)

      val getRecR = route(FakeRequest(GET, getResource)).get
      status(getRecR) should be (NOT_FOUND)

    }



  }

}
