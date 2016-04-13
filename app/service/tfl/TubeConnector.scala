package service.tfl


import com.sun.xml.internal.ws.api.server.SDDocument.WSDL
import model.TFLTubeService
import play.api.{Configuration, Play}
import play.api.libs.ws.WSClient
import repository.TubeRepository

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current

import scala.concurrent.Future

trait TubeConnector  {
  val ws:WSClient
  val configuration: Configuration

  def apiId = configuration.getString("tfl-api-id").getOrElse(throw new IllegalStateException("NO API ID found for TFL"))
  def apiKey = configuration.getString("tfl-api-key").getOrElse(throw new IllegalStateException("NO API KEY found for TFL"))


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


  def findTubeById(id: String) : Future[Option[TFLTubeService]] = {
    tubeRepository.findById(id)
  }

  def findTubeByIds(ids: Seq[String]): Future[Seq[Option[TFLTubeService]]] = {
    Future.sequence(ids.map(findTubeById))
  }

  val tubeRepository : TubeRepository

  def updateTubeServices: Future[Boolean] = {
    val tubeRecordsF = fetchLineStatus("tube")
    val dlrRecordsF = fetchLineStatus("dlr")
    val tflRailRecordsF = fetchLineStatus("tflrail")
    val overgroundRailRecordsF = fetchLineStatus("overground")

    for {
      tubeRecords <- tubeRecordsF
      dlrRecords <- dlrRecordsF
      overgroundRecords <- fetchLineStatus("overground")
      tflRailRecords <- tflRailRecordsF
      results <- tubeRepository.saveTubeService(tubeRecords ++ dlrRecords ++ overgroundRecords ++ tflRailRecords)
    } yield true
  }

}



