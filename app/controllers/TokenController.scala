package controllers

import helpers.Auth0Config
import play.api.Logger
import play.api.http.HeaderNames
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future

class TokenController extends Controller {


  def callback() = Action.async { implicit request =>

    val token = request.headers.get(HeaderNames.AUTHORIZATION)

    Logger.info(s"token - $token")

     token.fold(Future.successful(Unauthorized)){
       value =>
         Auth0Config.decodeAndVerifyToken(value.split("Bearer")(1)) match {
           case Right(token) => Future.successful(Ok)
           case Left(message) => Logger.info(message); Future.successful(Unauthorized)
         }
     }



  }


}
