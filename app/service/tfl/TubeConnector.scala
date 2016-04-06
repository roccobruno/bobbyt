package service.tfl

import com.sun.xml.internal.ws.api.server.SDDocument.WSDL
import model.TFLTubeService
import play.api.Play
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.http.{HttpGet, HttpPost}
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost}

import scala.concurrent.Future

trait TubeConnector extends HttpGet with HttpPost {

  def fetchLineStatus: Future[Seq[TFLTubeService]]

}

object TubeConnector extends TubeConnector with WSGet with WSPost{
  
  val apiId = Play.configuration.getString("tfl-api-id").getOrElse(throw new IllegalStateException("NO API ID found for TFL"))
  val apiKey = Play.configuration.getString("tfl-api-key").getOrElse(throw new IllegalStateException("NO API KEY found for TFL"))
  
  val appName = "jack"

  override def fetchLineStatus: Future[Seq[TFLTubeService]] = GET[Seq[TFLTubeService]](s"https://api.tfl.gov.uk/Line/Mode/tube/Status?detail=False&app_id=$apiId&app_key=$apiKey")

  override val hooks: Seq[HttpHook] = NoneRequired
}


trait TubeService {

  def tubeConnector: TubeConnector
  def tubeRepository : TubeRepository

  def updateTubeServices: Future[Boolean]

}

object TubeService extends TubeService {
  override def tubeConnector: TubeConnector = TubeConnector

  override def updateTubeServices: Future[Boolean] = {
    tubeConnector.fetchLineStatus map {
      records =>

    }
  }
}