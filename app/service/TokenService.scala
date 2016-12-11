package service

import model.Token
import repository.BobbitRepository
import util.FutureO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait TokenService {

  val bobbitRepository:BobbitRepository


  def validateToken(token: String): Future[Option[Token]] = {
    (for {
      token <- FutureO(bobbitRepository.findValidTokenByValue(token))
      delete <- FutureO(bobbitRepository.deleteById(token.getId))
      save <- FutureO(bobbitRepository.saveToken(token))
    } yield token).future
  }

  def deleteToken(token : String): Future[Option[Token]] = {

    (for {
      token <- FutureO(bobbitRepository.findValidTokenByValue(token))
      delete <- FutureO(bobbitRepository.deleteById(token.getId))
    }yield token).future

  }


}
