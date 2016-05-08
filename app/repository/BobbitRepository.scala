package repository

import java.util.UUID

import com.couchbase.client.protocol.views._
import model.{Job}
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
object BobbitRepository extends BobbitRepository {


  val driver = ReactiveCouchbaseDriver()
  override lazy val bucket = driver.bucket("default")
  override lazy val runningJobBucket = driver.bucket("runningJob")
  override lazy val alertsBucket = driver.bucket("alert")

  def createRunningJobDesignerDocument() = runningJobBucket.designDocument("runningJob") map {

    case res: DesignDocument => println("designer document present")
    case _ => {
      val desDoc = new DesignDocument("runningJob")
        desDoc.getViews.add(new ViewDesign("by_jobId",viewMapFunction))
        desDoc.getViews.add(new ViewDesign("by_time_and_recurring_alert_sent",viewByStartTimeMapFunction))
        desDoc.getViews.add(new ViewDesign("by_end_time_and_recurring_alert_sent",viewByEndTimeMapFunction))
      runningJobBucket.createDesignDoc(desDoc) map {
        {
          case o: OpResult if o.isSuccess => println("designer doc CREATED for runningJob table")
          case o: OpResult => Right(o.getMessage)
        }
      }

    }
  }

  def createAlertDesignerDocument() = runningJobBucket.designDocument("alert") map {

    case res: DesignDocument => println("designer document present")
    case _ => {
      val desDoc = new DesignDocument("alert")
      desDoc.getViews.add(new ViewDesign("by_id",viewByIdMapFunction))
      alertsBucket.createDesignDoc(desDoc) map {
        {
          case o: OpResult if o.isSuccess => println("designer doc CREATED")
          case o: OpResult => Right(o.getMessage)
        }
      }

    }
  }

  def createJobDesignerDocument() = runningJobBucket.designDocument("job") map {

    case res: DesignDocument => println("designer document present")
    case _ => {
      println("designer document not found")
      val desDoc = new DesignDocument("job")
      desDoc.getViews.add(new ViewDesign("by_id",viewByIdMapFunction))
      bucket.createDesignDoc(desDoc) map {
        {
          case o: OpResult if o.isSuccess => println("designer doc CREATED")
          case o: OpResult => Right(o.getMessage)
        }
      }

    }
  }

  val viewMapFunction =
    """
      |function (doc, meta) {
      |  emit(doc.jobId, null);
      |}
    """.stripMargin

  val viewByStartTimeMapFunction =
    """
      |function (doc, meta) {
      |  emit([doc.recurring,doc.alertSent,doc.from.time], null);
      |}
    """.stripMargin

  val viewByEndTimeMapFunction =
    """
      |function (doc, meta) {
      |  emit([doc.recurring,doc.alertSent,doc.to.time], null);
      |}
    """.stripMargin

  val viewByIdMapFunction =
    """
      |function (doc, meta) {
      |  emit(doc.id, null);
      |}
    """.stripMargin


  createRunningJobDesignerDocument()
  createAlertDesignerDocument()
  createJobDesignerDocument()
}


trait BobbitRepository {


  def driver:ReactiveCouchbaseDriver
  def bucket: CouchbaseBucket
  def runningJobBucket : CouchbaseBucket
  def alertsBucket : CouchbaseBucket

  def deleteAllRunningJob() = {
    deleteAll(findAllRunningJob,deleteRunningJoById)
  }

  def deleteAllJobs() = {
    deleteAll(findAllJob,deleteJobById)
  }

  def deleteAllAlerts() = {
    deleteAll(findAllAlert,deleteAlertById)
  }

  def deleteAll[T <: InternalId](findAll : () => Future[Seq[T]], delete: (String) =>Future[Either[String,Any]]) = {
    findAll() map {
      recs =>
        recs map (rec => delete(rec.getId))
    } recover {
      case _ => println("Error in deleting rows. Probably no rows were found")
    }
  }

  def saveAlert(alert: EmailAlert): Future[Either[String,Any]] = {
    val id = alert.getId
    alertsBucket.set[EmailAlert](id,alert) map {
      case o: OpResult if o.isSuccess => Left(id)
      case o: OpResult => Right(o.getMessage)
    }
  }

  def saveJob(job: Job): Future[Either[String,Any]] = {
    val id = job.getId
    bucket.set[Job](id, job) map {
      case o: OpResult if o.isSuccess => Left(id)
      case o: OpResult => Right(o.getMessage)
    }
  }

  def saveRunningJob(job: RunningJob): Future[Either[String,Any]] = {
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

  def deleteJobById(id:String) : Future[Either[String,Any]] = {
    bucket.delete(id) map {
      case o: OpResult if o.isSuccess => Left(id)
      case o: OpResult => Right(o.getMessage)
    }
  }

  def deleteAlertById(id:String) : Future[Either[String,Any]] = {
    alertsBucket.delete(id) map {
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


  def findRunningJobByJobId(jobId: String): Future[Option[RunningJob]] = {
    val query = new Query().setIncludeDocs(true).setLimit(1)
      .setRangeStart(ComplexKey.of(jobId)).setRangeEnd(ComplexKey.of(s"$jobId\uefff")).setStale(Stale.FALSE)
    runningJobBucket.find[RunningJob]("runningJob", "by_jobId")(query).map(_.headOption)
  }


  def findRunningJobToExecute() : Future[Set[RunningJob]] = {
    for {
      first <- findRunningJobToExecuteByStartTime()
      second <- findRunningJobToExecuteByEndTime()
    } yield (first ++ second).toSet
  }

  def findRunningJobToExecuteByStartTime() : Future[Seq[RunningJob]] = {
    val query = new Query().setIncludeDocs(true)
      .setRangeStart(ComplexKey.of(java.lang.Boolean.TRUE,java.lang.Boolean.FALSE,timeOfDay(DateTime.now))).
      setRangeEnd(ComplexKey.of(
        java.lang.Boolean.TRUE,java.lang.Boolean.FALSE,timeOfDay(DateTime.now.plusMinutes(30)))).setStale(Stale.FALSE)
    runningJobBucket.find[RunningJob]("runningJob", "by_time_and_recurring_alert_sent")(query)
  }

  def findRunningJobToExecuteByEndTime() : Future[Seq[RunningJob]] = {
    val query = new Query().setIncludeDocs(true)
      .setRangeStart(ComplexKey.of(java.lang.Boolean.TRUE,java.lang.Boolean.FALSE,timeOfDay(DateTime.now))).
      setRangeEnd(ComplexKey.of(
        java.lang.Boolean.TRUE,java.lang.Boolean.FALSE,timeOfDay(DateTime.now.plusMinutes(30)))).setStale(Stale.FALSE)
    runningJobBucket.find[RunningJob]("runningJob", "by_end_time_and_recurring_alert_sent")(query)
  }




  def findRunningJobToReset() : Future[Seq[RunningJob]] = {
    val query = new Query().setIncludeDocs(true)
      .setRangeStart(ComplexKey.of(java.lang.Boolean.TRUE,java.lang.Boolean.TRUE,new Integer("0"))).
      setRangeEnd(ComplexKey.of(java.lang.Boolean.TRUE,java.lang.Boolean.TRUE,timeOfDay(DateTime.now.minusHours(1)))).setStale(Stale.FALSE)
    runningJobBucket.find[RunningJob]("runningJob", "by_time_and_recurring_alert_sent")(query)
  }

  private def timeOfDay(tm: DateTime): Integer = TimeOfDay.time(tm.hourOfDay().get(),tm.minuteOfHour().get())

}
