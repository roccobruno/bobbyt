package scheduled

import java.util.concurrent.TimeUnit

import metrics.Metrics
import org.joda.time.{DateTime, Duration}
import play.api.Logger
import reactivemongo.api.DefaultDB
import repository.{JackRepository}
import uk.gov.hmrc.lock.{LockKeeper, LockRepository}
import uk.gov.hmrc.play.scheduling.ExclusiveScheduledJob

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

object ScheduledJob extends ScheduledJob {
  override val repo = JackRepository
  override def now = DateTime.now()
  override def jobConfiguration = JobConfiguration
  override lazy val metrics = Metrics
}

trait ScheduledJob extends ExclusiveScheduledJob {
  val name = "scheduledJob"
  def jobConfiguration: JobConfiguration
  def repo: JackRepository
  def metrics: Metrics
  def now: DateTime

  override lazy val initialDelay = FiniteDuration(jobConfiguration.nextExecutionInSeconds(now), TimeUnit.SECONDS)

  override lazy val interval = FiniteDuration(24, TimeUnit.HOURS)

  override def executeInMutex(implicit ec: ExecutionContext): Future[Result] =
      runJob

  private[scheduled] def runJob(implicit ec: ExecutionContext) = {
    Future.successful(Result("ok"))
  }
}

