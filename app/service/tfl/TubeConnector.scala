package service.tfl


import model.TFLTubeService
import play.api.{Configuration, Logger}
import play.api.libs.ws.WSClient
import repository.TubeRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait TubeConnector  {
  val ws:WSClient
  val configuration: Configuration

  def apiId = configuration.getString("tfl-api-id").getOrElse(throw new IllegalStateException("NO API ID found for TFL"))
  def apiKey = configuration.getString("tfl-api-key").getOrElse(throw new IllegalStateException("NO API KEY found for TFL"))


  def fetchLineStatus(lineType:String): Future[Seq[TFLTubeService]] = {
    Logger.info(s"connecting to TFL for type $lineType...")
    ws.url(s"https://api.tfl.gov.uk/Line/Mode/$lineType/Status?detail=False&app_id=$apiId&app_key=$apiKey").get() map {
      response =>
        response.json.validate[Seq[TFLTubeService]].fold(
          errs => throw new IllegalArgumentException(s"Error in parsing TUBE service response error:$errs"),
          valid => valid
        )
    } recover {
      case ex: Throwable => println(s"ERRROROROORROORORO ${ex.getMessage}"); Seq()
    }
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



