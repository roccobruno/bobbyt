package jobs

import javax.inject.Inject

import akka.actor.ActorSystem
import play.api.Configuration
import service.JobService
import service.tfl.TubeService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class JobsRegistry @Inject()(system: ActorSystem,
                             tubeService: TubeService,
                             jobService: JobService,
                             configuration: Configuration) {

  private val tubeServiceActor = system.actorOf(TubeServiceFetchActor.props(tubeService), "tubeServiceActor")
  private val tubeServiceCheckActor = system.actorOf(TubeServiceCheckerActor.props(jobService), "tubeServiceCheckerActor")
  private val alertJobActor = system.actorOf(ProcessAlertsJobActor.props(jobService), "alertJobActor")
  private val alertCleanerJobActor = system.actorOf(AlertCleanerJobActor.props(jobService), "alertCleanerJobActor")


  lazy val tubeScheduleJob = system.scheduler.schedule(
    0.microseconds, 10000.milliseconds, tubeServiceActor, Run("run"))

  lazy val tubeCheckerScheduleJob = system.scheduler.schedule(
    0.microseconds, 10000.milliseconds, tubeServiceCheckActor, Run("run"))

  lazy val alertJobScheduleJob = system.scheduler.schedule(
    0.microseconds, 10000.milliseconds, alertJobActor, Run("run"))

  lazy val alertCleanerJobScheduleJob = system.scheduler.schedule(
    0.microseconds, 1.hour, alertCleanerJobActor, Run("run"))

  def startJobs = {
    tubeScheduleJob
    tubeCheckerScheduleJob
    alertJobScheduleJob
    alertCleanerJobScheduleJob
  }

  def cancelAllJobs = {
    tubeScheduleJob.cancel
    tubeCheckerScheduleJob.cancel
    alertJobScheduleJob.cancel
    alertCleanerJobScheduleJob.cancel
  }

}
