package jobs

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit}
import akka.util.Timeout
import org.junit.runner.RunWith
import org.mockito.Mockito.{mock, when}
import org.specs2.runner.JUnitRunner
import play.api.Configuration
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import service.tfl.{TubeConnector, TubeService}
import util.Testing

import scala.concurrent.Future
import scala.concurrent.duration.Duration


@RunWith(classOf[JUnitRunner])
class TubeServiceFetchActorSpec  extends TestKit(ActorSystem("TubeServiceFetchActorSpec")) with Testing  {

  def url(lineType: String) = s"https://api.tfl.gov.uk/Line/Mode/$lineType/Status?detail=False&app_id=apiId&app_key=apiKey"


  def setUpMock(wsClientMock: WSClient)(lineType: String) = {
    val responseMock = mock(classOf[WSResponse])
    val wsRequest = mock(classOf[WSRequest])
    when(responseMock.json).thenReturn(Json.parse(line(lineType)))
    when(wsClientMock.url(url(lineType))).thenReturn(wsRequest)
    when(wsRequest.get()).thenReturn(Future.successful((responseMock)))
  }

  override protected def beforeAll(): Unit = {
    await(tubeRepository.deleteAllTubeLines())
  }

  def checkTubeLine: Unit = {
    val alertsCreated = await(tubeRepository.findAllTubeLines())
    alertsCreated.size shouldBe 4


  }

  def line(lineType: String) = s"""[
                                   {
                                     "id": "$lineType",
                                     "name": "$lineType",
                                     "modeName": "tube",
                                     "disruptions": [],
                                     "created": "2017-01-10T17:28:11.04Z",
                                     "modified": "2017-01-10T17:28:11.04Z",
                                     "lineStatuses": [
                                       {
                                         "id": 0,
                                         "statusSeverity": 10,
                                         "statusSeverityDescription": "Good Service",
                                         "created": "0001-01-01T00:00:00",
                                         "validityPeriods": []
                                       }
                                     ],
                                     "routeSections": [],
                                     "serviceTypes": [
                                       {
                                         "name": "Regular",
                                         "uri": "/Line/Route?ids=Bakerloo&serviceTypes=Regular"
                                       }
                                     ]
                                   }
                                 ]"""

  "an actor " should {

    "fetch and insert lines info" in {


      val wsMock = mock(classOf[WSClient])


      val setUpMocked = setUpMock(wsMock)_

      setUpMocked("tube")
      setUpMocked("dlr")
      setUpMocked("tflrail")
      setUpMocked("overground")

      val configurationMock: Configuration = mock(classOf[Configuration])

      when(configurationMock.getString("tfl-api-id")).thenReturn(Some("apiId"))
      when(configurationMock.getString("tfl-api-key")).thenReturn(Some("apiKey"))


      val tubeConnector = new TubeConnector(wsMock, configurationMock)
      val tubeService =  new TubeService(tubeRepository, tubeConnector)
      val actor = TestActorRef(new TubeServiceFetchActor(tubeService))




      val tubeLinesAfterActorBefore = await(tubeRepository.findAllTubeLines())
      tubeLinesAfterActorBefore.size shouldBe 0




      implicit val timeout: Timeout = Timeout(10000, TimeUnit.MILLISECONDS)
      val res = actor ! Run("test")


      awaitAssert(checkTubeLine, Duration(10, TimeUnit.SECONDS))




    }


  }


}
