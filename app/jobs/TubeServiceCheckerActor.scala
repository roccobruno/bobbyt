package jobs

import akka.actor._
import play.api.Logger
import service.JobService

import scala.concurrent.ExecutionContext.Implicits.global

object TubeServiceCheckerActor {
  def props(jobService: JobService ): Props = Props(new TubeServiceCheckerActor(jobService))

}

class TubeServiceCheckerActor(jobService: JobService) extends Actor {

  def receive = {
    case Run(name: String) =>

      jobService.checkTubeLinesAndCreatesAlert() map { res =>
        Logger.info(s"Running job : tube service checker. Generated ${res.flatten.size} Alerts")
        sender() ! res
      }
  }
}