package jobs

import akka.actor._
import service.JobService

import scala.concurrent.ExecutionContext.Implicits.global

object ResetRunningJobActor {
  def props(jobService: JobService): Props = Props(new ResetRunningJobActor(jobService))
}

class ResetRunningJobActor(jobService: JobService) extends Actor {

  def receive = {
    case Run(name: String) =>

      jobService.findAndResetActiveJobs() map { res =>
        println(s"Running job : reset job - n:${res.size}")
        sender() ! res
      }
  }
}