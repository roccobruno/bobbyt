package service

import javax.inject.{Inject, Singleton}

import model.Token
import repository.BobbytRepository
import util.FutureO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


@Singleton
class TokenService @Inject()(bobbytRepository: BobbytRepository) {

  def deleteToken(token : String): Future[Option[Token]] = {

    (for {
      token <- FutureO(bobbytRepository.findValidTokenByValue(token))
      delete <- FutureO(bobbytRepository.deleteById(token.getId))
    }yield token).future

  }


}
