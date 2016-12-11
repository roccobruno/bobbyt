package controllers

import model.Token
import play.api.http.HeaderNames
import play.api.libs.json.JsValue
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait Securing {

  self: BobbitController =>

  def authToken(implicit request: Request[_]) = {
    request.headers.get(HeaderNames.AUTHORIZATION)
  }

  case class AuthContext(token: Token,request: Request[_])

  def IsAuthenticated(f: AuthContext => Future[Result]) = {
      Action.async { implicit request =>
        request.headers.get(HeaderNames.AUTHORIZATION) match {
          case Some(token) => {
            TokenService.validateToken(token) flatMap  {
              case Some(validToken) => f(AuthContext(validToken,request))
              case _ => Future.successful(Forbidden)
            }
          }
          case _ => Future.successful(Forbidden)
        }
      }
  }

  def IsAuthenticatedWithJson(f: Request[JsValue] => Future[Result]) = {
    Action.async(parse.json) { implicit request =>
      request.headers.get(HeaderNames.AUTHORIZATION) match {
        case Some(token) => {
          TokenService.validateToken(token) flatMap  {
            case Some(validToken) => f(request)
            case _ => Future.successful(Forbidden)
          }
        }
        case _ => Future.successful(Forbidden)
      }
    }
  }
}
