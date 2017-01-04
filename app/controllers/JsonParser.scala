package controllers

import play.api.libs.json.{JsError, JsSuccess, JsValue, Reads}
import play.api.mvc.{Request, Result, Results}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait JsonParser {

  def withJsonBody[T](f: (T) => Future[Result])(implicit request: Request[JsValue], m: Manifest[T], reads: Reads[T]) =
    Try(request.body.validate[T]) match {
      case Success(JsSuccess(payload, _)) => f(payload)
      case Success(JsError(errs)) => Future.successful(Results.BadRequest(s"Invalid ${m.runtimeClass.getSimpleName} payload: $errs"))
      case Failure(e) => Future.successful(Results.BadRequest(s"could not parse body due to ${e.getMessage}"))
    }

}
