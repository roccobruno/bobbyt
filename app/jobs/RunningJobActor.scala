package jobs

import akka.actor._
import play.api.Logger
import service.tfl.{JobService, TubeService, TubeConnector}
import scala.concurrent.ExecutionContext.Implicits.global

object RunningJobActor {
  def props(jobService: JobService): Props = Props(new RunningJobActor(jobService))

}

class RunningJobActor(jobService: JobService) extends Actor {
  def receive = {
    case Run(name: String) =>

      jobService.findAndProcessActiveJobs() map { res =>
        println(s"Running job : process job - n:${res.size}")
        sender() ! res
      }
  }
}