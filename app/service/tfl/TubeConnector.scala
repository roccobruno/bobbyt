package service.tfl


import com.sun.xml.internal.ws.api.server.SDDocument.WSDL
import model.TFLTubeService
import play.api.Play
import play.api.libs.ws.WSClient
import repository.TubeRepository

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current

import scala.concurrent.Future

trait TubeConnector  {
  val ws:WSClient

  val apiId = Play.configuration.getString("tfl-api-id").getOrElse(throw new IllegalStateException("NO API ID found for TFL"))
  val apiKey = Play.configuration.getString("tfl-api-key").getOrElse(throw new IllegalStateException("NO API KEY found for TFL"))


  def fetchLineStatus(lineType:String): Future[Seq[TFLTubeService]] = ws.url(s"https://api.tfl.gov.uk/Line/Mode/$lineType/Status?detail=False&app_id=$apiId&app_key=$apiKey").get() map {
    response =>
      response.json.validate[Seq[TFLTubeService]].fold(
        errs => throw new IllegalArgumentException(s"Error in parsing TUBE service response error:$errs"),
        valid => valid
      )
  }

}



trait TubeService {
  this:TubeConnector =>


  val tubeRepository : TubeRepository

  def updateTubeServices: Future[Boolean] = {
    for {
      tubeRecords <- fetchLineStatus("tube")
      dlrRecords <- fetchLineStatus("dlr")
      overgroundRecords <- fetchLineStatus("overground")
      tflRailRecords <- fetchLineStatus("tflrail")
      results <- tubeRepository.saveTubeService(tubeRecords ++ dlrRecords ++ overgroundRecords ++ tflRailRecords)
    } yield true
  }

}



