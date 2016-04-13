package util

import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.{Await, Awaitable}
import scala.concurrent.duration._


trait Testing extends WordSpecLike with Matchers with BeforeAndAfterAll {

  def await[T](awaitable: Awaitable[T]) = {
    Await.result(awaitable,2 seconds)
  }

  def await[T](awaitable: Awaitable[T], duration : Duration) = {
    Await.result(awaitable,duration)
  }

}
