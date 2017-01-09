package model

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json, Writes}

trait InternalId {
  def getId: String
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


case class TimeOfDay(hour: Int, min: Int, time: Option[Int] = Some(0)) {

  require((min >= 0 && min <= 60), s"min : $min cannot be gr than 60")
  require((hour >= 0 && hour <= 24), s"hour : $hour cannot be gr than 24")

  def plusMinutes(mins: Int) = {
    val h = hour + (mins + min) / 60
    val m = (mins + min) % 60
    TimeOfDay(h, m, Some(TimeOfDay.time(h, m)))
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


case class EmailToSent(from: String, to: String, text:String, subject: Option[String] = Some("TEST"), htmlBody: Option[String])

object EmailToSent {
  implicit val format = Json.format[EmailToSent]
}

case class Email(nameFrom:String,from: EmailAddress,nameTo:String, to: EmailAddress)

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
case class Job(title:String,
               alert: Email,
               journey: Journey,
               private val id: Option[String] = Some(UUID.randomUUID().toString),
               active: Boolean = true,
               onlyOn: Option[DateTime] = None,
               docType: String = "Job",
               accountId: String) extends InternalId {
  def getId = this.id.get

}

object Job {
  implicit val format = Json.format[Job]
}






case class EmailAlert(private val id: String = UUID.randomUUID().toString,
                      email: Email,
                      persisted: DateTime,
                      sentAt: Option[DateTime],
                      jobId: String,
                      docType: String = "Alert",
                      sent: Boolean = false) extends InternalId {
  def getId = this.id
}

object EmailAlert {
  implicit val format = Json.format[EmailAlert]
}




case class Account(private val id: Option[String] = Some(UUID.randomUUID().toString),
                   userName: String,
                   firstName: Option[String] = None, //it might be none if user uses FB to login
                   lastName:Option[String] = None, //it might be none if user uses FB to login
                   email: Option[EmailAddress] = None,//it might be none if user uses FB to login
                   docType: String = "Account",
                   psw: Option[String], //it might be none if user uses FB to login
                   active:Boolean = true,
                   userId: Option[String] = None)
  extends InternalId {
  def getId = this.id.getOrElse(throw new IllegalStateException("found an Account without an ID!!!"))
}


object Account {
  implicit val format = Json.format[Account]
}


case class Token(private val id: Option[String] = Some(UUID.randomUUID().toString),
                 token : String,
                 accountId: Option[String] = None,
                 docType: String = "Token",
                 lastTimeUpdate: DateTime = DateTime.now(),
                 userId: String) extends InternalId {
  def getId = this.id.getOrElse(throw new IllegalStateException("found an Token without an ID!!!"))
}



object Token {
  implicit val format = Json.format[Token]
}

case class Login(private val id: Option[String] = Some(UUID.randomUUID().toString),
                 username : String, password: String ,
                 docType: String = "Login")extends InternalId {
  def getId = this.id.getOrElse(throw new IllegalStateException("found an Login without an ID!!!"))
}
object Login {
  implicit val format = Json.format[Login]
}