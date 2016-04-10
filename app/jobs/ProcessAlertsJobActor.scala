package jobs

import akka.actor.{Actor, Props}
import service.tfl.JobService
import scala.concurrent.ExecutionContext.Implicits.global

object ProcessAlertsJobActor{
  def props(jobService: JobService): Props = Props(new ProcessAlertsJobActor(jobService))
}

class ProcessAlertsJobActor(jobService: JobService) extends Actor {

  def receive = {
    case Run(name: String) =>
      jobService.processAlerts() map { res =>
        println(s"Running job : process alerts - n:${res.size}")
        sender() ! res
      }
  }
}