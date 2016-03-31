package repository

import java.util.UUID

import com.couchbase.client.protocol.views.{ComplexKey, Stale, Query}
import model.{Jack}
import org.reactivecouchbase.{ReactiveCouchbaseDriver, CouchbaseBucket}
import org.reactivecouchbase.play.PlayCouchbase
import play.api.libs.json._
import org.reactivecouchbase.client.OpResult
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
import scala.util.Failure


object JackRepository extends JackRepository {


  val driver = ReactiveCouchbaseDriver()
  override lazy val bucket = driver.bucket("default")

}


trait JackRepository {


  def bucket: CouchbaseBucket


  def saveABobby(bobby: Jack): Future[Either[String,Any]] = {
    val id = bobby.getId
    bucket.set[Jack](id, bobby) map {
      case o: OpResult if o.isSuccess => Left(id)
      case _ => Right()
    }
  }

  def findById(id: String): Future[Option[Jack]] = {
    bucket.get[Jack](id)
  }

  def deleteById(id:String) : Future[Either[String,Any]] = {
    bucket.delete(id) map {
      case o: OpResult if o.isSuccess => Left(id)
      case _ => Right()
    }
  }

  def findAll(): Future[List[Jack]] = {
    bucket.find[Jack]("bobby", "by_name")(new Query().setIncludeDocs(true).setStale(Stale.FALSE))
  }

  def findByName(name: String): Future[Option[Jack]] = {
    val query = new Query().setIncludeDocs(true).setLimit(1)
      .setRangeStart(ComplexKey.of(name)).setRangeEnd(ComplexKey.of(s"$name\uefff")).setStale(Stale.FALSE)
    bucket.find[Jack]("bobby", "by_name")(query).map(_.headOption)
  }


}
