package repository

import model.TFLTubeService
import org.joda.time.DateTime
import org.reactivecouchbase.client.OpResult
import org.reactivecouchbase.{CouchbaseBucket, ReactiveCouchbaseDriver}
import play.api.libs.iteratee.Enumerator
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current
import scala.concurrent.Future


object TubeRepository extends TubeRepository {


  val driver = ReactiveCouchbaseDriver()
  override lazy val bucket = driver.bucket("tube")

}

trait TubeRepository  {

  def bucket: CouchbaseBucket

  def saveTubeService(seq: Seq[TFLTubeService]): Future[Seq[OpResult]] = {
    Future.sequence(seq.map(service =>bucket.set[TFLTubeService](service.id,service.copy(lastUpdated = Some(DateTime.now)))))
  }

}


