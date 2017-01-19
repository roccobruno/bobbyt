package service

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import model.Token
import play.api.Configuration
import play.api.libs.ws.WSClient
import repository.{BobbytRepository, TubeRepository}
import util.FutureO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


@Singleton
class TokenService @Inject()(bobbytRepository: BobbytRepository) {


  def validateToken(token: String): Future[Option[Token]] = {
    (for {
      token <- FutureO(bobbytRepository.findValidTokenByValue(token))
      delete <- FutureO(bobbytRepository.deleteById(token.getId))
      save <- FutureO(bobbytRepository.saveToken(token))
    } yield token).future
  }

  def deleteToken(token : String): Future[Option[Token]] = {

    (for {
      token <- FutureO(bobbytRepository.findValidTokenByValue(token))
      delete <- FutureO(bobbytRepository.deleteById(token.getId))
    }yield token).future

  }


}
