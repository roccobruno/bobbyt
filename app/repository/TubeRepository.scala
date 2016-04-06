package repository

import model.TFLTubeService
import org.reactivecouchbase.{CouchbaseBucket, ReactiveCouchbaseDriver}
import play.api.libs.iteratee.Enumerator

import scala.concurrent.Future


object TubeRepository extends JackRepository {


  val driver = ReactiveCouchbaseDriver()
  override lazy val bucket = driver.bucket("tube")

}

trait TubeRepository  {

  def bucket: CouchbaseBucket

  def saveTubeService(seq: Seq[TFLTubeService]) = {
    seq.foreach(service =>bucket.set[TFLTubeService](service.id,service))
  }

}


