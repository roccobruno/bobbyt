package model

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.json.Json

case class Jack(private val id: String = UUID.randomUUID().toString, firstName: String, lastName: String) {
  def getId = this.id
}

object Jack {

  implicit val format = Json.format[Jack]
}


case class Station(name: String, crsCode: String)

case class TrainService(from: Station, to: Station)

case class TubeLine(name: String, id: String)

case class MeansOfTransportation(tubeLines: Seq[TubeLine], trainService: Seq[TrainService])

case class Journey(recurring: Boolean, meansOfTransportation: MeansOfTransportation, startsAt: DateTime, duration: Int)

case class Email(from: String, to: String)

/**
 *
 * @param alert alert to generate
 * @param journey journey to check
 * @param id record id
 * @param active
 * @param recurring if occurs every day
 * @param onlyOn indicates the date on the job must be executed - it cannot be a recurring one
 */
case class Job(alert: Email,
               journey: Journey,
               private val id: String = UUID.randomUUID().toString,
               active: Boolean,
               recurring: Boolean,
               onlyOn: Option[DateTime]) {
  def getId = this.id
}


case class EmailAlert(email: Email, persisted: Option[DateTime], sent: Option[DateTime])

case class JobForJack(runFrom: Int, runTill: Int, alertSent: Boolean, recurring: Boolean)
