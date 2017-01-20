package controllers

import java.util.UUID
import javax.inject.Inject

import akka.actor.ActorSystem
import model.{Account, Login, Token}
import play.api.Configuration
import play.api.http.HeaderNames
import play.api.libs.json.{JsString, Json}
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller, Cookie, Results}
import repository.{BobbytRepository, TubeRepository}
import service.{BearerTokenGenerator, TokenService}
import util.FutureO
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class AccountController @Inject()(system: ActorSystem, wsClient: WSClient, conf: Configuration, bobbytRepository: BobbytRepository, tubeRepository: TubeRepository, tokenService: TokenService) extends
  Controller with JsonParser with TokenChecker {
  override val repository: BobbytRepository = bobbytRepository



  def findAccount(id: String) = Action.async { implicit request =>
    WithAuthorization { token =>
      repository.findAccountById(id) map {
        case b: Some[Account] => Ok(Json.toJson[Account](b.get))
        case _ => NotFound
      }
    }
  }

  def findAccountByToken() = Action.async { implicit authContext =>
    WithAuthorization { token =>
      repository.findAccountByUserId(token.userId) map {
        case Some(account) => Ok(Json.toJson(account))
        case _ => NotFound
      }
    }
  }

  def checkUserName(userName: String) = Action.async { implicit request =>
    WithAuthorization { token =>
      repository.findAccountByUserName(userName) map {
        case head :: tail => Ok
        case Nil => NotFound
      }
    }
  }

  def validateAccount(token: String) = Action.async { implicit request =>
    WithValidToken(Some(token)) { jwtToken =>
      val res = (for {
        tk <- FutureO(repository.findValidTokenByValue(jwtToken.token))
        result <- FutureO(repository.activateAccount(tk, jwtToken.token))
      } yield result).future

      res map {
        case Some(id) => Redirect("/accountactive")
        case None => BadRequest(Json.obj("message" -> JsString("The supplied token is not valid or is expired")))
      }
    }
  }

  def validateToken(token: String) = Action.async {
    tokenService.validateToken(token) map {
      case Some(token) => Ok
      case _ => BadRequest
    }
  }

  //TODO send email to confirm account
  //TODO encrypt password before storing it
  def account() = Action.async(parse.json) { implicit request =>
    val accId = UUID.randomUUID().toString
    withJsonBody[Account] { account =>
      val accountToSave = account.copy(id = Some(accId), userId = Some(accId))
      for {
        account <- repository.saveAccount(accountToSave)
        token <- generateToken(accountToSave)
        _ <- repository.saveToken(Token(token = token.value, accountId = Some(accId), userId = accId))
      } yield Created.withHeaders(LOCATION -> ("/api/bobbyt/account/" + accId), HeaderNames.AUTHORIZATION -> s"Bearer ${token.value}")
    }
  }

  def submitProfile() = Action.async(parse.json) { implicit request =>
    val accId = UUID.randomUUID().toString
    WithAuthorization { jwtToken =>
      withJsonBody[Account] { account =>
        val accountToSave = account.copy(id = Some(accId), userId = Some(jwtToken.userId))
        for {
          account <- repository.saveAccount(accountToSave)
        } yield Created.withHeaders(LOCATION -> ("/api/bobbyt/account/" + accId), HeaderNames.AUTHORIZATION -> jwtToken.token)
      }
    }
  }

  def loginWithToken() = Action.async(parse.json) { implicit request =>
    WithValidToken {
      jwtToken =>
        repository.findTokenByUserId(jwtToken.userId) flatMap {
          case Some(token) => Future.successful(Results.Ok)
          case None => {
            repository.saveToken(Token(token = jwtToken.token, userId = jwtToken.userId)) map {
              case Some(id) => Results.Created
              case _ => Results.InternalServerError
            }

          }
        }
    }
  }

  def login() = Action.async(parse.json) { implicit request =>
    withJsonBody[Login] { login =>
      repository.findAccountByUserName(login.username) flatMap {
        case Seq(account) if (account.active && account.psw.getOrElse(UUID.randomUUID().toString) == login.password) => {
          for {
            token <- generateToken(account)
            res <- repository.saveToken(Token(token = token.value, accountId = Some(account.getId), userId = "")) map {
              case Some(id) => Ok.withHeaders(HeaderNames.AUTHORIZATION -> s"Bearer ${token.value}")
              case _ => println(s"Error saving token for account ${account.getId} in login"); InternalServerError
            }
          } yield (res)
        }
        case Seq(account) => Future.successful(Unauthorized)
        case Nil => Future.successful(NotFound)
      }

    }

  }

  def logout() = Action.async { implicit request =>
    request.headers.get(HeaderNames.AUTHORIZATION) match {
      case Some(token) => tokenService.deleteToken(token.split(" ")(1)) map {
        case _ => Ok.withCookies(Cookie("token", "", httpOnly = false))
      }
      case _ => Future.successful(Ok.withCookies(Cookie("token", "", httpOnly = false)))
    }
  }


}
