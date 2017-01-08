package controllers

import java.util.UUID
import javax.inject.Inject

import _root_.util.FutureO
import akka.actor.ActorSystem
import jobs._
import model.{Job, _}
import play.api.Configuration
import play.api.http.HeaderNames
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc._
import repository.{BobbytRepository, TubeRepository}
import service.tfl.{TubeConnector, TubeService}
import service.{BearerTokenGenerator, JobService, MailGunService, TokenService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._


class BobbytController @Inject()(system: ActorSystem, wsClient: WSClient, conf: Configuration) extends
  Controller with JsonParser with TokenChecker {
  val repository: BobbytRepository = BobbytRepository

  def getTubRepository = TubeRepository

  object TokenService extends TokenService {
    override val bobbytRepository: BobbytRepository = repository
  }

  object JobServiceImpl extends JobService {
    val repo = repository
    val ws = wsClient
    val tubeRepository = getTubRepository
    override val configuration: Configuration = conf
    override val mailGunService: MailGunService = MailGunService
  }

  object MailGunService extends MailGunService {
    override val ws: WSClient = wsClient

    override def mailGunApiKey = conf.getString("mailgun-api-key").getOrElse(throw new IllegalStateException("no configuration found for mailGun apiKey"))

    override def mailGunHost: String = conf.getString("mailgun-host").getOrElse(throw new IllegalStateException("no configuration found for mailGun host"))

    override def enableSender: Boolean = conf.getBoolean("mailgun-enabled").getOrElse(false)

  }

  object TubeServiceRegistry extends TubeService with TubeConnector {
    val ws = wsClient
    val tubeRepository = getTubRepository
    override val configuration: Configuration = conf
  }


  lazy val tubeServiceActor = system.actorOf(TubeServiceFetchActor.props(TubeServiceRegistry), "tubeServiceActor")
  lazy val tubeServiceCheckActor = system.actorOf(TubeServiceCheckerActor.props(JobServiceImpl), "tubeServiceCheckerActor")
  lazy val alertJobActor = system.actorOf(ProcessAlertsJobActor.props(JobServiceImpl), "alertJobActor")
  lazy val alertCleanerJobActor = system.actorOf(AlertCleanerJobActor.props(JobServiceImpl), "alertCleanerJobActor")


  lazy val tubeScheduleJob = system.scheduler.schedule(
    0.microseconds, 10000.milliseconds, tubeServiceActor, Run("run"))


  lazy val tubeCheckerScheduleJob = system.scheduler.schedule(
    0.microseconds, 10000.milliseconds, tubeServiceCheckActor, Run("run"))


  lazy val alertJobScheduleJob = system.scheduler.schedule(
    0.microseconds, 10000.milliseconds, alertJobActor, Run("run"))

  lazy val alertCleanerJobScheduleJob = system.scheduler.schedule(
    0.microseconds, 1.hour, alertCleanerJobActor, Run("run"))

  def fetchTubeLine() = Action.async { implicit request =>

    //          tubeScheduleJob
    tubeCheckerScheduleJob
    alertJobScheduleJob
    alertCleanerJobScheduleJob
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

  def findAccountByToken() = Action.async { implicit authContext =>
    WithAuthorization { token =>
      repository.findAccountById(token.userId) map {
        case Some(account) => Ok(Json.toJson(account))
        case _ => NotFound
      }
    }
  }

  def deleteAll() = Action.async {
    implicit request =>

      for {
        del <- repository.deleteAllAlerts()
        del <- repository.deleteAllJobs()
        del <- repository.deleteAllAccount()
      } yield Ok
  }

  def delete(id: String) = Action.async {
    implicit request =>
      repository.deleteById(id) map {
        case Some(id) => Ok
        case _ => InternalServerError
      }
  }

  def deleteRunningJob(id: String) = Action.async {
    implicit request =>
      repository.deleteById(id) map {
        case Some(id) => Ok
        case _ => InternalServerError
      }
  }

  def findAccount(id: String) = Action.async { implicit request =>
    WithAuthorization { token =>
      repository.findAccountById(id) map {
        case b: Some[Account] => Ok(Json.toJson[Account](b.get))
        case _ => NotFound
      }
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

  def checkUserName(userName: String) = Action.async { implicit request =>
    WithAuthorization { token =>
        repository.findAccountByUserName(userName) map {
        case head :: tail => Ok
        case Nil => NotFound
      }
    }
  }

  def validateAccount() = Action.async { implicit request =>
    WithAuthorization { token =>
      val res = (for {
        tk <- FutureO(repository.findValidTokenByValue(token.token))
        result <- FutureO(repository.activateAccount(tk, token.token))
      } yield result).future

      res map {
        case Some(id) => Redirect("/accountactive")
        case None => BadRequest(Json.obj("message" -> JsString(""))) //TODO
      }
    }
  }

  def validateToken(token: String) = Action.async {
    TokenService.validateToken(token) map {
      case Some(token) => Ok
      case _ => BadRequest
    }
  }

  //TODO send email to confirm account
  def account() = Action.async(parse.json) { implicit request =>
    val accId = UUID.randomUUID().toString
    withJsonBody[Account] { account =>
      val accountToSave = account.copy(id = Some(accId))
      for {
        account <- repository.saveAccount(accountToSave)
        token <- generateToken(accountToSave)
        _ <- repository.saveToken(Token(token = token.value, accountId = Some(accId), userId = ""))
      } yield Created.withHeaders(LOCATION -> ("/api/bobbyt/account/" + accId), HeaderNames.AUTHORIZATION -> s"Bearer ${token.value}")
    }
  }

  def submitProfile() = Action.async(parse.json) { implicit request =>
    val accId = UUID.randomUUID().toString
    WithAuthorization { jwtToken =>
      withJsonBody[Account] { account =>
        val accountToSave = account.copy(id = Some(accId))
        for {
          account <- repository.saveAccount(accountToSave)
        } yield Created.withHeaders(LOCATION -> ("/api/bobbyt/account/" + accId), HeaderNames.AUTHORIZATION -> jwtToken.token)
      }
    }
  }

  def loginWithToken() = Action.async(parse.json) { implicit request =>
    WithAuthorization {
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
        case Seq(account) if (account.active && account.psw == login.password) => {
          val token = BearerTokenGenerator.generateSHAToken("account-token")
          repository.saveToken(Token(token = token, accountId = Some(account.getId), userId = "")) map {
            case Some(id) => Ok.withCookies(Cookie("token", token, httpOnly = false))
            case _ => println(s"Error saving token for account ${account.getId} in login"); InternalServerError
          }
        }
        case Seq(account) => Future.successful(Unauthorized)
        case Nil => Future.successful(NotFound)
      }

    }

  }

  def logout() = Action.async { implicit request =>
    request.headers.get(HeaderNames.AUTHORIZATION) match {
      case Some(token) => TokenService.deleteToken(token.split(" ")(1)) map {
        case _ => Ok.withCookies(Cookie("token", "", httpOnly = false))
      }
      case _ => Future.successful(Ok.withCookies(Cookie("token", "", httpOnly = false)))
    }
  }


}


