package controllers

import play.api.libs.json.{JsSuccess, Reads, JsValue}
import play.api.mvc.{Results, Request, Result}

import scala.concurrent.Future

trait JsonParser {

  def withJsonBody[T](f: T => Future[Result])(implicit request: Request[JsValue], rds: Reads[T]) :Future[Result] = {
    request.body.validate[T] match {
      case res: JsSuccess[T] => f(res.get)
      case _ => Future.successful(Results.BadRequest)
    }
  }

}
