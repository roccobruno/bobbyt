package controllers

import java.util.UUID
import javax.inject.Inject

import _root_.util.FutureO
import akka.actor.ActorSystem
import jobs._
import model.{Job, _}
import play.api.{Configuration, Logger}
import play.api.http.HeaderNames
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc._
import repository.{BobbytRepository, TubeRepository}
import service.tfl.{TubeConnector, TubeService}
import service.{BearerTokenGenerator, JobService, MailGunService, TokenService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._


class BobbytController @Inject()(system: ActorSystem, wsClient: WSClient, conf: Configuration, bobbytRepository: BobbytRepository, tubeRepository: TubeRepository, tokenService: TokenService) extends
  Controller with JsonParser with TokenChecker {
  val repository: BobbytRepository = bobbytRepository

  def getTubRepository = tubeRepository

  object JobServiceImpl extends JobService {
    val repo = repository
    val ws = wsClient
    val tubeRepository = getTubRepository
    override val configuration: Configuration = conf
    override val mailGunService: MailGunService = MailGunService
  }

  object MailGunService extends MailGunService {
    override val ws: WSClient = wsClient

    override def mailGunApiKey = conf.getString("mailgun-api-key").getOrElse(throw new IllegalStateException("no configuration found for mailGun apiKey"))

    override def mailGunHost: String = conf.getString("mailgun-host").getOrElse(throw new IllegalStateException("no configuration found for mailGun host"))

    override def enableSender: Boolean = conf.getBoolean("mailgun-enabled").getOrElse(false)

  }

  object TubeServiceRegistry extends TubeService with TubeConnector {
    val ws = wsClient
    val tubeRepository = getTubRepository
    override val configuration: Configuration = conf
  }


  lazy val tubeServiceActor = system.actorOf(TubeServiceFetchActor.props(TubeServiceRegistry), "tubeServiceActor")
  lazy val tubeServiceCheckActor = system.actorOf(TubeServiceCheckerActor.props(JobServiceImpl), "tubeServiceCheckerActor")
  lazy val alertJobActor = system.actorOf(ProcessAlertsJobActor.props(JobServiceImpl), "alertJobActor")
  lazy val alertCleanerJobActor = system.actorOf(AlertCleanerJobActor.props(JobServiceImpl), "alertCleanerJobActor")


  lazy val tubeScheduleJob = system.scheduler.schedule(
    0.microseconds, 10000.milliseconds, tubeServiceActor, Run("run"))


  lazy val tubeCheckerScheduleJob = system.scheduler.schedule(
    0.microseconds, 10000.milliseconds, tubeServiceCheckActor, Run("run"))


  lazy val alertJobScheduleJob = system.scheduler.schedule(
    0.microseconds, 10000.milliseconds, alertJobActor, Run("run"))

  lazy val alertCleanerJobScheduleJob = system.scheduler.schedule(
    0.microseconds, 1.hour, alertCleanerJobActor, Run("run"))

  def fetchTubeLine() = Action.async { implicit request =>

    //          tubeScheduleJob
    tubeCheckerScheduleJob
    alertJobScheduleJob
    alertCleanerJobScheduleJob
    Future.successful(Ok(Json.obj("res" -> true)))

  }

  def find(id: String) = Action.async { implicit request =>
    WithAuthorization { token =>
      repository.findJobById(id) map {
        case b: Some[Job] => Ok(Json.toJson[Job](b.get))
        case _ => NotFound
      }
    }
  }

  def findAllJobByToken() = Action.async { implicit request =>
    WithAuthorization { token =>
      repository.findAllJobByAccountId(token.userId) map {
        case list: Seq[Job] => Ok(Json.toJson(list))
      }
    }
  }


  def delete(id: String) = Action.async {
    implicit request =>
      repository.deleteById(id) map {
        case Some(id) => Ok
        case _ => InternalServerError
      }
  }



  def save() = Action.async(parse.json) { implicit request =>
    WithAuthorization { token =>
      val jobId = UUID.randomUUID().toString
      withJsonBody[Job] { job =>
        val jobToSave = job.copy(id = Some(jobId), accountId = token.userId)
        for {
          Some(id) <- repository.saveJob(jobToSave)
        } yield Created.withHeaders("Location" -> ("/api/bobbyt/" + id))

      }
    }
  }

  def updateJob() = Action.async(parse.json) { implicit request =>
    WithAuthorization { token =>
      withJsonBody[Job] { job =>
        val jobToSave = job.copy(accountId = token.userId)
        for {
          Some(id) <- repository.saveJob(jobToSave)
        } yield Ok.withHeaders("Location" -> ("/api/bobbyt/" + id))

      }
    }
  }




}


