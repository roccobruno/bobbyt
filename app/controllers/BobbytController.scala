package controllers

import java.util.UUID
import javax.inject.Inject

import akka.actor.ActorSystem
import helpers.Auth0Config
import jobs._
import model.Job
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc._
import repository.{BobbytRepository, TubeRepository}
import service.tfl.TubeService
import service.{JobService, MailGunService, TokenService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._


class BobbytController @Inject()(system: ActorSystem,
                                 wsClient: WSClient,
                                 conf: Configuration,
                                 bobbytRepository: BobbytRepository,
                                 tubeRepository: TubeRepository,
                                 tokenService: TokenService,
                                 mailGunService: MailGunService,
                                 tubeService: TubeService,
                                 jobService: JobService,
                                 auth0Configuration: Auth0Config,
                                 jobsRegistry: JobsRegistry) extends Controller with JsonParser with TokenChecker {

  override val repository: BobbytRepository = bobbytRepository
  override val auth0Config = auth0Configuration

  def getTubRepository = tubeRepository


  def fetchTubeLine() = Action.async { implicit request =>

    jobsRegistry.startJobs
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


