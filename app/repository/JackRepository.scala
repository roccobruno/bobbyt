package repository

import java.util.UUID

import com.couchbase.client.protocol.views.{DesignDocument, ComplexKey, Stale, Query}
import model.{Job, Jack}
import org.joda.time.DateTime
import org.reactivecouchbase.{ReactiveCouchbaseDriver, CouchbaseBucket}
import org.reactivecouchbase.play.PlayCouchbase
import play.api.libs.json._
import org.reactivecouchbase.client.OpResult
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global
import org.reactivecouchbase.play.plugins.CouchbaseN1QLPlugin._

import scala.concurrent.Future
import scala.util.Failure

import model._
object JackRepository extends JackRepository {


  val driver = ReactiveCouchbaseDriver()
  override lazy val bucket = driver.bucket("default")
  override lazy val runningJobBucket = driver.bucket("runningJob")
  override lazy val alertsBucket = driver.bucket("alert")

}


trait JackRepository {


  def driver:ReactiveCouchbaseDriver
  def bucket: CouchbaseBucket
  def runningJobBucket : CouchbaseBucket
  def alertsBucket : CouchbaseBucket

  def deleteAllRunningJob() = {
//
//    findAllRunningJob map {
//      recs =>
//        recs map (rec => deleteRunningJoById(rec.getId))
//    } recover {
//      case _ => println("Error in deleting rows. Problably no rows were found")
//    }

    deleteAll(findAllRunningJob,deleteRunningJoById)

  }

  def deleteAll[T <: InternalId](findAll : () => Future[Seq[T]], delete: (String) =>Future[Either[String,Any]]) = {
    findAll() map {
      recs =>
        recs map (rec => delete(rec.getId))
    } recover {
      case _ => println("Error in deleting rows. Problably no rows were found")
    }
  }

  def saveAlert(alert: EmailAlert): Future[Either[String,Any]] = {
    val id = UUID.randomUUID().toString
    alertsBucket.set[EmailAlert](id,alert) map {
      case o: OpResult if o.isSuccess => Left(id)
      case o: OpResult => Right(o.getMessage)
    }
  }

  def saveAJackJob(job: Job): Future[Either[String,Any]] = {
    val id = job.getId
    bucket.set[Job](id, job) map {
      case o: OpResult if o.isSuccess => Left(id)
      case o: OpResult => Right(o.getMessage)
    }
  }

  def saveRunningJackJob(job: RunningJob): Future[Either[String,Any]] = {
    val id = job.getId
    runningJobBucket.set[RunningJob](id, job) map {
      case o: OpResult if o.isSuccess => Left(id)
      case o: OpResult => Right(o.getMessage)
    }
  }

  def findRunningJobById(id: String): Future[Option[RunningJob]] = {
    runningJobBucket.get[RunningJob](id)
  }

  def findById(id: String): Future[Option[Job]] = {
    bucket.get[Job](id)
  }

  def deleteById(id:String) : Future[Either[String,Any]] = {
    bucket.delete(id) map {
      case o: OpResult if o.isSuccess => Left(id)
      case o: OpResult => Right(o.getMessage)
    }
  }

  def deleteRunningJoById(id:String) : Future[Either[String,Any]] = {
    runningJobBucket.delete(id) map {
      case o: OpResult if o.isSuccess => Left(id)
      case o: OpResult => Right(o.getMessage)
    }
  }

  def findAllRunningJob(): Future[List[RunningJob]] = {
    runningJobBucket.find[RunningJob]("runningJob", "by_jobId")(new Query().setIncludeDocs(true).setStale(Stale.FALSE))
  }

  def findAllAlert(): Future[List[EmailAlert]] = {
    alertsBucket.find[EmailAlert]("alert", "by_id")(new Query().setIncludeDocs(true).setStale(Stale.FALSE))
  }

  def findAllJob(): Future[List[Job]] = {
    alertsBucket.find[Job]("default", "by_id")(new Query().setIncludeDocs(true).setStale(Stale.FALSE))
  }

  def findByName(name: String): Future[Option[Jack]] = {
    val query = new Query().setIncludeDocs(true).setLimit(1)
      .setRangeStart(ComplexKey.of(name)).setRangeEnd(ComplexKey.of(s"$name\uefff")).setStale(Stale.FALSE)
    bucket.find[Jack]("jack", "by_name")(query).map(_.headOption)
  }


  def findRunningJobByJobId(jobId: String): Future[Option[RunningJob]] = {
    val query = new Query().setIncludeDocs(true).setLimit(1)
      .setRangeStart(ComplexKey.of(jobId)).setRangeEnd(ComplexKey.of(s"$jobId\uefff")).setStale(Stale.FALSE)
    runningJobBucket.find[RunningJob]("runningJob", "by_jobId")(query).map(_.headOption)
  }


  def findRunningJobToExecute() : Future[Seq[RunningJob]] = {
    val query = new Query().setIncludeDocs(true)
      .setRangeStart(ComplexKey.of(java.lang.Boolean.TRUE,java.lang.Boolean.FALSE,timeOfDay(DateTime.now))).
      setRangeEnd(ComplexKey.of(
        java.lang.Boolean.TRUE,java.lang.Boolean.FALSE,timeOfDay(DateTime.now.plusMinutes(10)))).setStale(Stale.FALSE)
    runningJobBucket.find[RunningJob]("runningJob", "by_time_and_recurring_alert_sent")(query)
  }

  def findRunningJobToReset() : Future[Seq[RunningJob]] = {
    val query = new Query().setIncludeDocs(true)
      .setRangeStart(ComplexKey.of(java.lang.Boolean.TRUE,java.lang.Boolean.TRUE,new Integer("0"))).
      setRangeEnd(ComplexKey.of(java.lang.Boolean.TRUE,java.lang.Boolean.TRUE,timeOfDay(DateTime.now.minusHours(1)))).setStale(Stale.FALSE)
    runningJobBucket.find[RunningJob]("runningJob", "by_time_and_recurring_alert_sent")(query)
  }

  private def timeOfDay(tm: DateTime): Integer = TimeOfDay.time(tm.hourOfDay().get(),tm.minuteOfHour().get())

}
