package scheduled

import org.joda.time.{DateTime, Seconds}
import play.api.Play

object JobConfiguration extends JobConfiguration  {

  import play.api.Play.current

  val name = "scheduledJob"
  override lazy val schedule: String = Play.configuration.getString(s"microservice.$name.schedule")
    .getOrElse(throw new Exception(s"microservice.$name.schedule is not set in config"))
  override lazy val enabled: Boolean = Play.configuration.getBoolean(s"microservice.$name.enabled")
    .getOrElse(throw new Exception(s"microservice.$name.enabled is not set in config"))
}

trait JobConfiguration {
  def schedule: String
  def enabled: Boolean

  lazy val hour: Int = schedule.split(":")(0).toInt
  lazy val minute: Int = schedule.split(":")(1).toInt
  def nextExecutionInSeconds(now: DateTime) = {
    Seconds.secondsBetween(now, nextExecution(now, hour, minute)).getSeconds
  }

  def nextExecution(now: DateTime, hour: Int, minute: Int) = {
    val next = now
      .withHourOfDay(hour)
      .withMinuteOfHour(minute)

    if (next.isBefore(now)) next.plusHours(24) else next
  }

}