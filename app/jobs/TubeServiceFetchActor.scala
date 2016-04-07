package jobs

import akka.actor._
import play.api.Logger
import service.tfl.{TubeService, TubeConnector}
import scala.concurrent.ExecutionContext.Implicits.global
case class Run(name: String)

object TubeServiceFetchActor {
  def props(tubeService: TubeService with TubeConnector): Props = Props(new TubeServiceFetchActor(tubeService))

}

class TubeServiceFetchActor(tubeService: TubeService with TubeConnector) extends Actor {

  def receive = {
    case Run(name: String) =>

      tubeService.updateTubeServices map { res =>
        println("Running job :tube service")
        sender() ! res
      }
  }
}