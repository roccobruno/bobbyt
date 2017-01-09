package service

import model.Token
import repository.BobbytRepository
import util.FutureO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait TokenService {

  val bobbytRepository: BobbytRepository


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
