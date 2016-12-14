package repository

import java.util.concurrent.TimeUnit

import com.couchbase.client.java.query.N1qlQuery
import com.couchbase.client.java.{AsyncBucket, CouchbaseCluster}
import model.{Job, _}
import org.asyncouchbase.index.IndexApi
import org.asyncouchbase.model.OpsResult
import org.asyncouchbase.query.Expression._
import org.asyncouchbase.query.{ANY, SELECT, SimpleQuery}
import org.joda.time.DateTime
import org.reactivecouchbase.CouchbaseExpiration.{CouchbaseExpirationTiming, CouchbaseExpirationTiming_byDuration}
import org.reactivecouchbase.N1QLQuery
import org.reactivecouchbase.client.Constants
import play.api.Logger
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.runtime.universe._

case class ID(id: String)
object ID {
  implicit val format = Json.format[ID]
}

object BobbitRepository extends BobbitRepository {


  val cluster = ClusterConfiguration.cluster
  val bucket = new IndexApi {
    override def asyncBucket: AsyncBucket = cluster.openBucket("bobbit").async()
  }


  //by_type_token
  bucket.createPrimaryIndex(deferBuild = false) map {
    _ => Logger.info("PRIMARY INDEX CREATED")
  } recover {
    case ed: Throwable => Logger.error(s"PRIMARY INDEX NOT CREATED error= ${ed.getMessage}")
  }

  bucket.createIndex(Seq("token", "docType"), deferBuild = false) map {

    _ => Logger.info("SECONDARY INDEX CREATED")
  } recover {
    case ed: Throwable => Logger.error(s"SECONDARY INDEX CREATED error= ${ed.getMessage}")
  }




}


trait BobbitRepository {

  implicit val validateQuery = false

  def cluster:CouchbaseCluster

  def bucket: IndexApi

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

  def activateAccount(token: Token, tokenValue: String): Future[Option[String]] = {
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

  def save[T <: InternalId](entity: T)(implicit expirationTime: CouchbaseExpirationTiming = Constants.expiration, writes: Writes[T]):
  Future[Option[String]] = {
    val id = entity.getId
    bucket.upsert[T](id, entity) map {
      case o: OpsResult if o.isSuccess => Some(id)
      case o: OpsResult => println(s"error in saving entity: $entity - opResult:${o.msg}"); None
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

  def findById[T](id: String)(implicit rds: Reads[T]): Future[Option[T]] = bucket.get[T](id)

  def findValidTokenByValue(token: String): Future[Option[Token]] = {
    val query =  SELECT ("*") FROM "bobbit" WHERE ("token" === token AND "docType" === "Token")
    bucket.find[Token](query) map {
      case head:: tail => Some(head)
      case Nil => None
    }

  }

  def findAccountByUserName(userName: String): Future[List[Account]] = {

    val query =  SELECT ("*") FROM "bobbit" WHERE ("userName" === userName AND "docType" === "Account")
    bucket.find[Account](query)

  }


  def deleteById(id: String): Future[Option[String]] = {

    bucket.delete(id) map {
      case o: OpsResult if o.isSuccess => Some(id)
      case o: OpsResult => println(s"error in deleting object with id: $id, opResult:${o.msg}"); None
    }
  }


  def findAllByType[T: TypeTag](docType: String)(implicit rds: Reads[T]): Future[List[T]] = {
    val query =  SELECT ("*") FROM "bobbit" WHERE ("docType" === docType)
    bucket.find[T](query)
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

    val query =  SELECT ("*") FROM "bobbit" WHERE ("docType" === "Job" AND "accountId" === accountId)
    bucket.find[Job](query)
  }

  def findRunningJobByJobId(jobId: String): Future[Option[RunningJob]] = {
    val query =  SELECT ("*") FROM "bobbit" WHERE ("docType" === "RunningJob" AND "jobId" === jobId)
    bucket.find[RunningJob](query) map {
      case head :: tail => Some(head)
      case Nil => None
    }
  }


  def findRunningJobToExecute(): Future[Set[RunningJob]] = {
    for {
      first <- findRunningJobToExecuteByStartTime()
      second <- findRunningJobToExecuteByEndTime()
    } yield (first ++ second).toSet
  }

  def findRunningJobToExecuteByStartTime(): Future[Seq[RunningJob]] = {

    val now: DateTime = DateTime.now()
    val timeFrom = timeOfDay(now)
    val timeTO = timeOfDay(now.plusMinutes(30))

    val query =  SELECT ("*") FROM "bobbit" WHERE
      ("docType" === "RunningJob" AND "recurring" === true AND "alertSent" === false AND
        ( "fromTime.time" BETWEEN (timeFrom AND  timeTO)))

    bucket.find[RunningJob](query)

  }

  def findRunningJobToExecuteByEndTime(): Future[Seq[RunningJob]] = {

    val now: DateTime = DateTime.now()
    val timeFrom = timeOfDay(now)
    val timeTO = timeOfDay(now.plusMinutes(30))


    val query =  SELECT ("*") FROM "bobbit" WHERE
      ("docType" === "RunningJob" AND "recurring" === true AND "alertSent" === false AND
        ( "toTime.time" BETWEEN (timeFrom AND  timeTO)))

    bucket.find[RunningJob](query)

  }


  def findRunningJobToReset(): Future[Seq[RunningJob]] = {

    val now: DateTime = DateTime.now()
    val timeTO = timeOfDay(now.minusHours(1))

    val query =  SELECT ("*") FROM "bobbit" WHERE
      ("docType" === "RunningJob" AND "recurring" === true AND "alertSent" === true AND ("toTime.time" gt timeTO))

    bucket.find[RunningJob](query)


  }


  def findJobsByTubeLineAndRunningTime(tubeLines: Seq[TubeLine], runningTime: DateTime = DateTime.now()): Future[Seq[ID]] = {


    val tDay = timeOfDay(runningTime) - 30
    val fDay = timeOfDay(runningTime) +30


    val arrayTubeLines = s"[${tubeLines.map(d => s"'${d.id}'").mkString(",")}]"

    val query = SELECT("id") FROM "bobbit" WHERE (("docType" === "Job") AND ( "journey.startsAt.time" BETWEEN (tDay AND fDay))).AND( ANY("line") IN ("journey.meansOfTransportation.tubeLines") SATISFIES ("line.id" IN arrayTubeLines) END)

    bucket.find[ID](query)

  }

  private def timeOfDay(tm: DateTime): Int = TimeOfDay.time(tm.hourOfDay().get(), tm.minuteOfHour().get())

}


