package repository

import java.util.UUID
import java.util.concurrent.TimeUnit
import javafx.scene.control.Alert

import com.couchbase.client.protocol.views._
import model.{Job}
import org.joda.time.DateTime
import org.reactivecouchbase.CouchbaseExpiration.{CouchbaseExpirationTiming, CouchbaseExpirationTiming_byDuration, CouchbaseExpirationTiming_byInt}
import org.reactivecouchbase.{ReactiveCouchbaseDriver, CouchbaseBucket}
import org.reactivecouchbase.play.PlayCouchbase
import play.api.libs.json._
import org.reactivecouchbase.client.{Constants, OpResult}
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global
import org.reactivecouchbase.play.plugins.CouchbaseN1QLPlugin._

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.{Try, Failure}

import model._

object BobbitRepository extends BobbitRepository {


  val driver = ReactiveCouchbaseDriver()
  override lazy val bobbitBucket = driver.bucket("bobbit")


  def createBobbitDesignerDocument() = bobbitBucket.designDocument("bobbit") map {

    case res: DesignDocument => println("designer document present")
    case _ => {
      val desDoc = new DesignDocument("bobbit")
      desDoc.getViews.add(new ViewDesign("by_Id", viewByIdMapFunction))
      desDoc.getViews.add(new ViewDesign("by_type", viewByDoctypeMapFunction))
      desDoc.getViews.add(new ViewDesign("by_type_accountId", viewByDoctypeAndAccountIdMapFunction))
      desDoc.getViews.add(new ViewDesign("by_type_username", viewByTypeAndUsernameMapFunction))
      desDoc.getViews.add(new ViewDesign("by_type_token", viewByTypeAndTokenMapFunction))
      bobbitBucket.createDesignDoc(desDoc) map {
        {
          case o: OpResult if o.isSuccess => println("designer doc CREATED for RunningJob table")
          case o: OpResult => Right(o.getMessage)
        }
      }

    }
  }

  def createRunningJobDesignerDocument() = bobbitBucket.designDocument("runningJob") map {

    case res: DesignDocument => println("designer document present")
    case _ => {
      val desDoc = new DesignDocument("runningJob")
      desDoc.getViews.add(new ViewDesign("by_Id", viewByIdMapFunction))
      desDoc.getViews.add(new ViewDesign("by_type_jobId", viewMapFunctionByTypeAndJobId))
      desDoc.getViews.add(new ViewDesign("by_type_time_and_recurring_alert_sent", viewByStartTimeMapFunction))
      desDoc.getViews.add(new ViewDesign("by_type_end_time_and_recurring_alert_sent", viewByEndTimeMapFunction))
      bobbitBucket.createDesignDoc(desDoc) map {
        {
          case o: OpResult if o.isSuccess => println("designer doc CREATED for RunningJob table")
          case o: OpResult => Right(o.getMessage)
        }
      }

    }
  }


  val viewMapFunctionByTypeAndJobId =
    """
      |function (doc, meta) {
      |  if(doc.jobId) {
      |    emit([doc.docType,doc.jobId], null);
      |  }
      |}
    """.stripMargin

  val viewByStartTimeMapFunction =
    """
      |function (doc, meta) {
      |  emit([doc.docType,doc.recurring,doc.alertSent,doc.from.time], null);
      |}
    """.stripMargin

  val viewByEndTimeMapFunction =
    """
      |function (doc, meta) {
      |  emit([doc.docType,doc.recurring,doc.alertSent,doc.to.time], null);
      |}
    """.stripMargin

  val viewByIdMapFunction =
    """
      |function (doc, meta) {
      |  emit(doc.id, null);
      |}
    """.stripMargin

  val viewByDoctypeMapFunction =
    """
      |function (doc, meta) {
      |  emit(doc.docType, null);
      |}
    """.stripMargin

  val viewByDoctypeAndAccountIdMapFunction =
    """
      |function (doc, meta) {
      |  if(doc.accountId) {
      |  emit([doc.docType, doc.accountId], null);
      |  }
      |}
    """.stripMargin

  val viewByTypeAndUsernameMapFunction =
    """
      |function (doc, meta) {
      | if(doc.userName) {
      |  emit([doc.docType,doc.userName], null);
      |  }
      |}
    """.stripMargin

  val viewByTypeAndTokenMapFunction =
    """
      |function (doc, meta) {
      | if(doc.token) {
      |  emit([doc.docType,doc.token], null);
      |  }
      |}
    """.stripMargin



  createBobbitDesignerDocument()
  createRunningJobDesignerDocument()
}


trait BobbitRepository {


  def driver: ReactiveCouchbaseDriver

  def bobbitBucket: CouchbaseBucket


  def deleteAllRunningJob() = deleteAll(findAllRunningJob)


  def deleteAllJobs() = {
    deleteAll(findAllJob)
  }

  def deleteAllAccount() = {
    deleteAll(findAllAccount)
  }

  def deleteAllToken() = {
    deleteAll(findAllToken)
  }

  def deleteAllAlerts() = {
    deleteAll(findAllAlert)
  }


  def deleteAll[T <: InternalId](findAll: () => Future[Seq[T]]) = {
    findAll() map {
      recs =>
        recs map (rec => deleteById(rec.getId))
    } recover {
      case _ => println("Error in deleting rows. Probably no rows were found")
    }
  }

  def activateAccount(token: Token,tokenValue: String): Future[Option[String]] = {
      for {
        acc <- findById[Account](token.accountId)
        resultSaving <- saveAccount(acc, token.accountId)
        result <- deleteById(token.getId)
      } yield result

  }

  def saveAccount(account: Option[Account], accountId: String): Future[Option[String]] = {
    account match {
      case None => Future.successful(Some(s"no account found for id=$accountId"))
      case Some(acc) => saveAccount(acc.copy(active = true))
    }
  }

  def save[T <: InternalId](entity: T)(implicit expirationTime: CouchbaseExpirationTiming = Constants.expiration,writes: Writes[T]): 
  Future[Option[String]] = {
    val id = entity.getId
    bobbitBucket.set[T](id,entity, exp = expirationTime) map {
      case o: OpResult if o.isSuccess => Some(id)
      case o: OpResult => println(s"error in saving entity: $entity - opResult:${o.getMessage}"); None
    }
  }

  def saveAlert(alert: EmailAlert): Future[Option[String]] = save[EmailAlert](alert)

  def saveJob(job: Job): Future[Option[String]] = save[Job](job)

  def saveRunningJob(job: RunningJob): Future[Option[String]] = save[RunningJob](job)

  def saveToken(token: Token): Future[Option[String]] = {
    implicit val expirationTiming = CouchbaseExpirationTiming_byDuration(Duration.create(30, TimeUnit.MINUTES))
    save[Token](token)
  }

  def saveAccount(account: Account): Future[Option[String]] = save[Account](account)


  def findRunningJobById(id: String): Future[Option[RunningJob]] = findById[RunningJob](id)

  def findJobById(id: String): Future[Option[Job]] = findById[Job](id)

  def findAccountById(id: String): Future[Option[Account]] = findById[Account](id)

  def findById[T](id: String)(implicit rds: Reads[T]): Future[Option[T]] = bobbitBucket.get[T](id)

  def findValidTokenByValue(token: String) :Future[Option[Token]] = {
    bobbitBucket.find[Token]("bobbit", "by_type_token")(new Query().setIncludeDocs(true).setLimit(1)
      .setRangeStart(ComplexKey.of("Token",token)).
      setRangeEnd(ComplexKey.of("Token",s"$token\uefff")).
      setStale(Stale.FALSE)) map {
      case Seq(token) => Some(token)
      case _ => None
    }

  }

  def findAccountByUserName(userName: String): Future[List[Account]] = {
    bobbitBucket.find[Account]("bobbit", "by_type_username")(new Query().setIncludeDocs(true).setLimit(1)
      .setRangeStart(ComplexKey.of("Account",userName)).
      setRangeEnd(ComplexKey.of("Account",s"$userName\uefff")).
      setStale(Stale.FALSE))
  }

  def findTokenBy(token: String): Future[Option[Token]] = {
    bobbitBucket.find[Token]("bobbit", "by_type_token")(new Query().setIncludeDocs(true).setLimit(1)
      .setRangeStart(ComplexKey.of("Token",token)).
      setRangeEnd(ComplexKey.of("Token",s"$token\uefff")).
      setStale(Stale.FALSE)) map {
      case head :: Nil => Some(head)
      case _ => None
    }
  }

  def deleteById(id: String): Future[Option[String]] = {
    bobbitBucket.delete(id) map {
      case o: OpResult if o.isSuccess => Some(id)
      case o: OpResult => println(s"error in deleting object with id: $id, opResult:${o.getMessage}"); None
    }
  }


  def findAllByType[T <: InternalId](docType: String)(implicit rds: Reads[T]): Future[List[T]] = {
    bobbitBucket.find[T]("bobbit", "by_type")(new Query().setIncludeDocs(true)
      .setRangeStart(ComplexKey.of(docType)).
      setRangeEnd(ComplexKey.of(s"$docType\uefff")).
      setStale(Stale.FALSE))
  }

  def findAllRunningJob(): Future[List[RunningJob]] = {
    findAllByType[RunningJob]("RunningJob")
  }

  def findAllAlert(): Future[List[EmailAlert]] = {
    findAllByType[EmailAlert]("Alert")
  }

  def findAllAccount(): Future[List[Account]] = {
    findAllByType[Account]("Account")
  }

  def findAllToken(): Future[List[Token]] = {
    findAllByType[Token]("Token")
  }

  def findAllJob(): Future[List[Job]] = {
    findAllByType[Job]("Job")
  }

  def findAllJobByAccountId(accountId: String): Future[List[Job]] = {
    val query = new Query().setIncludeDocs(true)
      .setRangeStart(ComplexKey.of("Job", accountId)).setRangeEnd(ComplexKey.of("Job", s"$accountId\uefff")).setStale(Stale.FALSE)
    bobbitBucket.find[Job]("bobbit", "by_type_accountId")(query)
  }


  def findRunningJobByJobId(jobId: String): Future[Option[RunningJob]] = {
    val query = new Query().setIncludeDocs(true).setLimit(1)
      .setRangeStart(ComplexKey.of("RunningJob", jobId)).setRangeEnd(ComplexKey.of("RunningJob", s"$jobId\uefff")).setStale(Stale.FALSE)
    bobbitBucket.find[RunningJob]("runningJob", "by_type_jobId")(query).map(_.headOption)
  }


  def findRunningJobToExecute(): Future[Set[RunningJob]] = {
    for {
      first <- findRunningJobToExecuteByStartTime()
      second <- findRunningJobToExecuteByEndTime()
    } yield (first ++ second).toSet
  }

  def findRunningJobToExecuteByStartTime(): Future[Seq[RunningJob]] = {
    val query = new Query().setIncludeDocs(true)
      .setRangeStart(ComplexKey.of("RunningJob", java.lang.Boolean.TRUE, java.lang.Boolean.FALSE, timeOfDay(DateTime.now))).
      setRangeEnd(ComplexKey.of("RunningJob",
        java.lang.Boolean.TRUE, java.lang.Boolean.FALSE, timeOfDay(DateTime.now.plusMinutes(30)))).setStale(Stale.FALSE)
    bobbitBucket.find[RunningJob]("runningJob", "by_type_time_and_recurring_alert_sent")(query)
  }

  def findRunningJobToExecuteByEndTime(): Future[Seq[RunningJob]] = {
    val query = new Query().setIncludeDocs(true)
      .setRangeStart(ComplexKey.of("RunningJob", java.lang.Boolean.TRUE, java.lang.Boolean.FALSE, timeOfDay(DateTime.now))).
      setRangeEnd(ComplexKey.of("RunningJob",
        java.lang.Boolean.TRUE, java.lang.Boolean.FALSE, timeOfDay(DateTime.now.plusMinutes(30)))).setStale(Stale.FALSE)
    bobbitBucket.find[RunningJob]("runningJob", "by_type_end_time_and_recurring_alert_sent")(query)
  }


  def findRunningJobToReset(): Future[Seq[RunningJob]] = {
    val query = new Query().setIncludeDocs(true)
      .setRangeStart(ComplexKey.of("RunningJob", java.lang.Boolean.TRUE, java.lang.Boolean.TRUE, new Integer("0"))).
      setRangeEnd(ComplexKey.of("RunningJob", java.lang.Boolean.TRUE, java.lang.Boolean.TRUE, timeOfDay(DateTime.now.minusHours(1)))).setStale(Stale.FALSE)
    bobbitBucket.find[RunningJob]("runningJob", "by_type_time_and_recurring_alert_sent")(query)
  }

  private def timeOfDay(tm: DateTime): Integer = TimeOfDay.time(tm.hourOfDay().get(), tm.minuteOfHour().get())

}
