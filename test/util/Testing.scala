package util

import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import play.api.Configuration
import play.api.test.WithApplication
import repository.{BobbytRepository, ClusterConfiguration, TubeRepository}

import scala.concurrent.duration._
import scala.concurrent.{Await, Awaitable}


trait Testing extends  WordSpecLike with Matchers with BeforeAndAfterAll {

  val configuration = ConfigFactory.load()
  val bobbytRepository = new BobbytRepository(new ClusterConfiguration(Configuration(configuration)))
  val tubeRepository = new TubeRepository(new ClusterConfiguration(Configuration(configuration)))


  def await[T](awaitable: Awaitable[T]) = {
    Await.result(awaitable,10 seconds)
  }

  def await[T](awaitable: Awaitable[T], duration : Duration) = {
    Await.result(awaitable,duration)
  }

}
