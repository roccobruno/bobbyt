package model

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Writes, Json}


trait InternalId {
  def getId: String
}

case class Bobbit(private val id: String = UUID.randomUUID().toString, firstName: String, lastName: String) {
  def getId = this.id
}

object Bobbit {

  implicit val format = Json.format[Bobbit]
}


case class Station(name: String, crsCode: String)

object Station {
  implicit val format = Json.format[Station]
}

case class TrainService(from: Station, to: Station)

object TrainService {
  implicit val format = Json.format[TrainService]
}

case class TubeLine(name: String, id: String)

object TubeLine {
  implicit val format = Json.format[TubeLine]
}

case class MeansOfTransportation(tubeLines: Seq[TubeLine], trainService: Seq[TrainService])

object MeansOfTransportation {
  implicit val format = Json.format[MeansOfTransportation]
}


case class TimeOfDay(hour: Int, min: Int, time: Int = 0) {

  require((min >= 0 && min <= 60), s"min : $min cannot be gr than 60")
  require((hour >= 0 && hour <= 24), s"hour : $hour cannot be gr than 24")

  def plusMinutes(mins: Int) = {
    val h = hour + (mins + min) / 60
    val m = (mins + min) % 60
    TimeOfDay(h, m, TimeOfDay.time(h, m))
  }
}

object TimeOfDay {

  def time(hour: Int, min: Int) = {

    def minToStrin = if (min < 10) s"0$min" else min.toString

    (hour.toString + minToStrin).toInt
  }

  def plusMinutes(mins: Int) = TimeOfDay

  implicit val format = Json.reads[TimeOfDay]
  implicit val writeformat = new Writes[TimeOfDay] {

    override def writes(o: TimeOfDay): JsValue = Json.obj("hour" -> o.hour, "min" -> o.min, "time" -> time(o.hour, o.min))
  }

}


case class Journey(recurring: Boolean, meansOfTransportation: MeansOfTransportation, startsAt: TimeOfDay, durationInMin: Int)

object Journey {
  implicit val format = Json.format[Journey]
}


case class EmailToSent(from: String, to: EmailAddress, body:String, subject: Option[String], htmlBody: Option[String])

object EmailToSent {
  implicit val format = Json.format[EmailToSent]
}

case class Email(from: String, to: String)

object Email {
  implicit val format = Json.format[Email]
}

/**
 *
 * @param alert alert to generate
 * @param journey journey to check
 * @param id record id
 * @param active
 * @param onlyOn indicates the date on the job must be executed - it cannot be a recurring one
 */
case class Job(alert: Email,
               journey: Journey,
               private val id: String = UUID.randomUUID().toString,
               active: Boolean = true,
               onlyOn: Option[DateTime] = None) extends InternalId {
  def getId = this.id
}

object Job {
  implicit val format = Json.format[Job]
}


case class RunningJob(private val id: String = UUID.randomUUID().toString,
                      from: TimeOfDay, to: TimeOfDay, alertSent: Boolean = false,
                      recurring: Boolean = true, jobId: String)
  extends InternalId {
  def getId = this.id
}

object RunningJob {

  def fromJob(job: Job) = RunningJob(from = job.journey.startsAt, to = job.journey.startsAt.plusMinutes(job.journey.durationInMin), jobId = job.getId, recurring = job.journey.recurring)

  implicit val format = Json.format[RunningJob]
}

case class EmailAlert(private val id: String = UUID.randomUUID().toString,email: Email, persisted: Option[DateTime], sent: Option[DateTime], jobId: String) extends InternalId {
  def getId = this.id
}

case class JobForBobbit(runFrom: Int, runTill: Int, alertSent: Boolean, recurring: Boolean)

object EmailAlert {
  implicit val format = Json.format[EmailAlert]
}

object JobForBobbit {
  implicit val format = Json.format[JobForBobbit]
}