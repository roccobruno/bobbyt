package controllers

import helpers.{Auth0Config, EncodedJwtToken, JwtToken}
import io.igl.jwt._
import model.Account
import org.joda.time.DateTime
import play.api.Logger
import play.api.http.HeaderNames
import play.api.mvc._
import play.mvc.BodyParser.AnyContent

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


trait TokenChecker {

  def WithAuthorization(body : (JwtToken) => Future[Result])(implicit request: Request[_]) = {
    val token = request.headers.get(HeaderNames.AUTHORIZATION)
    token.fold(Future.successful[Result](Results.Unauthorized)){
      value =>
        Auth0Config.decodeAndVerifyToken(value.split(" ")(1)) match {
          case Right(token) => body(token)
          case Left(message) => Logger.warn(s"Token validation failed . Msg - $message");Future.successful(Results.Unauthorized)
        }
    }
  }

  def generateToken(account: Account) : Future[EncodedJwtToken] = {

    val expiry: Long = DateTime.now().plusHours(1).getMillis
    val created = DateTime.now().getMillis
    val jwt = new DecodedJwt(Seq(Alg(Algorithm.HS256), Typ("JWT")),
      Seq(Iss(account.getId), Sub(account.userName), Aud(""), Exp(expiry), Iat(created)))
    Future.successful(EncodedJwtToken(jwt.encodedAndSigned(Auth0Config.get().secret)))

  }

}
