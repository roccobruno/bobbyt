package jobs

import akka.actor._
import play.api.Logger
import service.tfl.{TubeConnector, TubeService}

import scala.concurrent.ExecutionContext.Implicits.global
case class Run(name: String)

object TubeServiceFetchActor {
  def props(tubeService: TubeService): Props = Props(new TubeServiceFetchActor(tubeService))

}

class TubeServiceFetchActor(tubeService: TubeService) extends Actor {

  def receive = {
    case Run(name: String) =>

      tubeService.updateTubeServices map { res =>
        println("Running job : tube service")
        sender() ! res
      }
  }
}