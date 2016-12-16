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

case class ID(id: String) extends InternalId {
  override def getId: String = id
}
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


  def findAllAlertSentYesterday() = {
    val time = DateTime.now().minusDays(1)
    val query = SELECT("id") FROM "bobbit" WHERE ("docType" === "Alert" AND "sent" === true AND ("sentAt" lt time))

    println(query)
    bucket.find[ID](query)
  }


  implicit val validateQuery = false

  def cluster:CouchbaseCluster

  def bucket: IndexApi

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
        recs map  (rec => deleteById(rec.getId))
    } recover {
      case _ => Logger.info("Error in deleting rows. Probably no rows were found")
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

  def saveAlertIfAbsent(alert: EmailAlert): Future[Option[String]] = {

    findAlertByJobId(alert.jobId) flatMap  {
      case Some(alert) => Future.successful(None)
      case None =>  saveAlert(alert) map {
        res => res
      }
    }
  }



  def saveJob(job: Job): Future[Option[String]] = save[Job](job)


  def saveToken(token: Token): Future[Option[String]] = {
    implicit val expirationTiming = CouchbaseExpirationTiming_byDuration(Duration.create(30, TimeUnit.MINUTES))
    save[Token](token)
  }

  def saveAccount(account: Account): Future[Option[String]] = save[Account](account)



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


  def findAllAlert(): Future[List[EmailAlert]] = {
    findAllByType[EmailAlert]("Alert")
  }

  def findAllAlertNotSent(): Future[List[EmailAlert]] = {
    val query =  SELECT ("*") FROM "bobbit" WHERE ("docType" === "Alert" AND "sent" === false)
    bucket.find[EmailAlert](query)
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


  def findJobsByTubeLineAndRunningTime(tubeLines: Seq[TubeLine], runningTime: DateTime = DateTime.now()): Future[Seq[Job]] = {


    val tDay = timeOfDay(runningTime) - 30
    val fDay = timeOfDay(runningTime) +30


    val arrayTubeLines = s"[${tubeLines.map(d => s"'${d.id}'").mkString(",")}]"

    val query = SELECT("*") FROM "bobbit" WHERE (("docType" === "Job") AND ( "journey.startsAt.time" BETWEEN (tDay AND fDay))).AND( ANY("line") IN ("journey.meansOfTransportation.tubeLines") SATISFIES ("line.id" IN arrayTubeLines) END)

    bucket.find[Job](query)

  }

  def findAlertByJobIdAndSentValue(jobId: String, sent: Boolean = false): Future[Option[EmailAlert]] = {

    val query = SELECT("*") FROM "bobbit" WHERE ("docType" === "Alert" AND "jobId" === jobId AND "sent" === sent)
    bucket.find[EmailAlert](query) map {
      case head :: tail => Some(head)
      case Nil => None
    }

  }

  def findAlertByJobId(jobId: String): Future[Option[EmailAlert]] = {

    val query = SELECT("*") FROM "bobbit" WHERE ("docType" === "Alert" AND "jobId" === jobId)
    bucket.find[EmailAlert](query) map {
      case head :: tail => Some(head)
      case Nil => None
    }

  }

  def markAlertAsSent(alertId: String) = {
    bucket.setValue(alertId, "sent", true)
  }

  def markAlertAsSentAt(alertId: String) = {
    bucket.setValue(alertId, "sentAt", DateTime.now().toDate)
  }

  private def timeOfDay(tm: DateTime): Int = TimeOfDay.time(tm.hourOfDay().get(), tm.minuteOfHour().get())

}


