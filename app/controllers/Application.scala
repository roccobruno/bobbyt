package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import jobs.{Run, HelloActor}
import org.reactivecouchbase.ReactiveCouchbaseDriver
import play.api._
import play.api.libs.json.JsObject
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global

class Application @Inject() (system: ActorSystem)  extends Controller {

  val helloActor = system.actorOf(HelloActor.props, "hello-actor")


  def index = Action {
    helloActor ! Run("Rocco")
    Ok(views.html.index("Your new application is ready."))
  }

  // get a driver instance driver
  val driver = ReactiveCouchbaseDriver()
  // get the default bucket
  val bucket = driver.bucket("default")

  // creates a JSON document
  val document = Json.obj(
    "name" -> "John",
    "surname" -> "Doe",
    "age" -> 42,
    "address" -> Json.obj(
      "number" -> "221b",
      "street" -> "Baker Street",
      "city" -> "London"
    )
  )

  // persist the JSON doc with the key 'john-doe', using implicit 'jsObjectToDocumentWriter' for serialization
  bucket.set[JsObject]("john-doe", document).onSuccess {
    case status => println(s"Operation status : ${status.getMessage}")
  }


  // shutdown the driver (only at app shutdown)
  driver.shutdown()

}
