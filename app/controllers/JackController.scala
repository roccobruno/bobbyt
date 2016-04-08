package controllers

import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

import akka.actor.ActorSystem
import jobs.{Run, TubeServiceFetchActor, HelloActor}
import model.{Job, Jack}
import org.reactivecouchbase.client.OpResult
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc.{Request, Result, Action, Controller}
import repository.{TubeRepository, JackRepository}
import service.tfl.{TubeConnector, TubeService}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import model._


class JackController  @Inject() (system: ActorSystem, wsClient:WSClient) extends Controller with JsonParser {

  def repository: JackRepository = JackRepository

  object TubeServiceRegistry extends TubeService with TubeConnector {
    val ws = wsClient
    val tubeRepository = TubeRepository
  }
  lazy  val tubeServiceActor = system.actorOf(TubeServiceFetchActor.props(TubeServiceRegistry), "tubeServiceActor")


  lazy val cancellable = system.scheduler.schedule(
    0.microseconds, 10000.milliseconds, tubeServiceActor,  Run("tick"))


  def fetchTubeLine() = Action.async { implicit request =>

      cancellable
      Future.successful(Ok(Json.obj("res"->true)))

  }

  def find(id: String) = Action.async { implicit request =>
    repository.findById(id) map {
      case b: Some[Job] => Ok(Json.toJson[Job](b.get))
      case _ => NotFound
    }
  }

  def delete(id: String) = Action.async {
    implicit request =>
      repository.deleteById(id) map {
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



  def save() = Action.async(parse.json) { implicit request =>
    withJsonBody[Job](jackJob =>

      for {
        Left(id) <- repository.saveAJackJob(jackJob)
        runningId <- repository.saveARunningJackJob(RunningJob.fromJob(jackJob))
      }  yield Created.withHeaders("Location" -> ("/api/jack/" + id))

    )
  }


}


