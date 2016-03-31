package controllers

import java.util.UUID

import model.{Jack}
import org.reactivecouchbase.client.OpResult
import play.api.libs.json._
import play.api.mvc.{Request, Result, Action, Controller}
import repository.JackRepository
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future


class JackController extends Controller with JsonParser {

  def repository: JackRepository = JackRepository

  def find(id: String) = Action.async { implicit request =>
    repository.findById(id) map {
      case b: Some[Jack] => Ok(Json.toJson[Jack](b.get))
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


  def save() = Action.async(parse.json) { implicit request =>
    withJsonBody[Jack](bobby =>
      repository.saveABobby(bobby).map {
        case Left(id) => Created.withHeaders("Location" -> ("/api/bobby/" + id))
        case _ => InternalServerError
      }
    )
  }


}


