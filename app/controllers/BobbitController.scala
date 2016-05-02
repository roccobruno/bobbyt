package controllers

import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

import akka.actor.ActorSystem
import jobs._
import model.{Job}
import org.reactivecouchbase.client.OpResult
import play.api.{Configuration, Environment}
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc.{Request, Result, Action, Controller}
import repository.{TubeRepository, BobbitRepository}
import service.{MailGunService, JobService}
import service.tfl.{TubeConnector, TubeService}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import model._


class BobbitController  @Inject() (system: ActorSystem, wsClient:WSClient, conf: Configuration) extends Controller with JsonParser {
  val repository: BobbitRepository = BobbitRepository
  def getTubRepository = TubeRepository

  object JobServiceImpl extends JobService {
    val repo = repository
    val ws = wsClient
    val tubeRepository = getTubRepository
    override val configuration: Configuration = conf
    override val mailGunService: MailGunService = MailGunService
  }

  object MailGunService extends MailGunService {
    override val ws: WSClient = wsClient

    override def mailGunApiKey = conf.getString("mailgun-api-key").getOrElse( throw new IllegalStateException("no configuration found for mailGun apiKey"))

    override def mailGunHost: String = conf.getString("mailgun-host").getOrElse( throw new IllegalStateException("no configuration found for mailGun host"))

    override def enableSender: Boolean =  conf.getBoolean("mailgun-enabled").getOrElse(false)

  }

  object TubeServiceRegistry extends TubeService with TubeConnector {
    val ws = wsClient
    val tubeRepository = getTubRepository
    override val configuration: Configuration = conf
  }


  lazy  val tubeServiceActor = system.actorOf(TubeServiceFetchActor.props(TubeServiceRegistry), "tubeServiceActor")
  lazy  val runningActor = system.actorOf(RunningJobActor.props(JobServiceImpl), "runningJobActor")
  lazy  val resetRunningJobActor = system.actorOf(ResetRunningJobActor.props(JobServiceImpl), "resetRunningJobActor")
  lazy  val alertJobActor = system.actorOf(ProcessAlertsJobActor.props(JobServiceImpl), "alertJobActor")


  lazy val tubeScheduleJob = system.scheduler.schedule(
    0.microseconds, 10000.milliseconds, tubeServiceActor,  Run("run"))

  lazy val runningJobScheduleJob = system.scheduler.schedule(
    0.microseconds, 10000.milliseconds, runningActor,  Run("run"))

  lazy val resetRunningJobScheduleJob = system.scheduler.schedule(
    0.microseconds, 10000.milliseconds, resetRunningJobActor,  Run("run"))

  lazy val alertJobScheduleJob = system.scheduler.schedule(
    0.microseconds, 10000.milliseconds, alertJobActor,  Run("run"))


  def fetchTubeLine() = Action.async { implicit request =>

//      tubeScheduleJob
      runningJobScheduleJob
      resetRunningJobScheduleJob
      alertJobScheduleJob
      Future.successful(Ok(Json.obj("res"->true)))

  }

  def find(id: String) = Action.async { implicit request =>
    repository.findById(id) map {
      case b: Some[Job] => Ok(Json.toJson[Job](b.get))
      case _ => NotFound
    }
  }

  def deleteAll() = Action.async {
    implicit request =>

      for {
        _ <-repository.deleteAllAlerts()
        _ <-repository.deleteAllJobs()
        _ <-repository.deleteAllRunningJob()
      }  yield Ok
  }

  def delete(id: String) = Action.async {
    implicit request =>
      repository.deleteJobById(id) map {
        case Left(id) => Ok
        case _ => InternalServerError
      }
  }

  def deleteRunningJob(id: String) = Action.async {
    implicit request =>
      repository.deleteRunningJoById(id) map {
        case Left(id) => Ok
        case _ => InternalServerError
      }
  }

  def findRunningJobByJobId(jobId: String) = Action.async { implicit request =>
    repository.findRunningJobByJobId(jobId) map {
      case b: Some[RunningJob] => Ok(Json.toJson[RunningJob](b.get))
      case _ => NotFound
    }
  }


  def findActiveRunningJob() = Action.async { implicit request =>
    JobServiceImpl.findAndProcessActiveJobs() map {
      case recs => Ok(Json.toJson[Seq[RunningJob]](recs))
    }

  }


  def save() = Action.async(parse.json) { implicit request =>
    val jobId = UUID.randomUUID().toString
    withJsonBody[Job]{job =>
      val jobToSave = job.copy(id = Some(jobId))
      for {
        Left(id) <- repository.saveJob(jobToSave)
        runningId <- repository.saveRunningJob(RunningJob.fromJob(jobToSave))
      }  yield Created.withHeaders("Location" -> ("/api/bobbit/" + id))

    }
  }


}


