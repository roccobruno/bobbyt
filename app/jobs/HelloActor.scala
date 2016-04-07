package jobs

import akka.actor._
import play.api.Logger

case class SayHello(name: String)

object HelloActor {
  def props = Props[HelloActor]

}

class HelloActor extends Actor {

  def receive = {
    case Run(name: String) =>
      println("ciao "+name)
      sender() ! "Hello, " + name
  }
}