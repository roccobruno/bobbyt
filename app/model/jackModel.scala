package model

import java.util.UUID

import play.api.libs.json.Json

case class Jack(private val id: String = UUID.randomUUID().toString, firstName: String, lastName: String) {
  def getId = this.id
}

object Jack {

  implicit val format = Json.format[Jack]
}

//Account
//FBAccount
//Journey
//JourneyMeansOfTransportation
//Alert
//EmailAlert
//
// ++ TFL model
// ++ TRAIN model