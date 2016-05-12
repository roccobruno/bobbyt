package controllers

import play.api.libs.json.JsValue
import play.api.mvc._

import scala.concurrent.Future

trait Securing {

  self: Controller =>

  def IsAuthenticated(f: Request[AnyContent] => Future[Result]) = {
      Action.async( implicit request => f(request))
  }

  def IsAuthenticatedWithJson(f: Request[JsValue] => Future[Result]) = {
    Action.async(parse.json)( implicit request => f(request))
  }
}
