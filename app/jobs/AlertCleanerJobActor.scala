package jobs

import akka.actor.{Actor, Props}
import play.api.Logger
import service.JobService

import scala.concurrent.ExecutionContext.Implicits.global

object AlertCleanerJobActor{
  def props(jobService: JobService): Props = Props(new AlertCleanerJobActor(jobService))
}


class AlertCleanerJobActor(jobService: JobService) extends Actor {

  def receive = {
    case Run(name: String) =>
      jobService.deleteSentAlerts() map { res =>
        Logger.info(s"Running job : cleaning alerts ")
        sender() ! res
      }
  }
}
