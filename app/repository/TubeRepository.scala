package repository

import com.couchbase.client.java.{AsyncBucket, CouchbaseCluster}
import model.TFLTubeService
import org.asyncouchbase.bucket.BucketApi
import org.asyncouchbase.index.IndexApi
import org.asyncouchbase.model.OpsResult
import org.joda.time.DateTime
import org.reactivecouchbase.client.OpResult
import org.reactivecouchbase.{CouchbaseBucket, ReactiveCouchbaseDriver}
import play.api.libs.iteratee.Enumerator

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current

import scala.concurrent.Future


object TubeRepository extends TubeRepository {


  val driver = ReactiveCouchbaseDriver()

  val cluster = CouchbaseCluster.create()
  val bucket = new IndexApi {
    override def asyncBucket: AsyncBucket = cluster.openBucket("tube").async()
  }

}

trait TubeRepository  {

  def bucket: BucketApi

  def findById(id: String) = {
    bucket.get[TFLTubeService](id)
  }

  def saveTubeService(seq: Seq[TFLTubeService]): Future[Seq[OpsResult]] = {
    Future.sequence(seq.map(service => bucket.upsert[TFLTubeService](service.id,service.copy(lastUpdated = Some(DateTime.now)))))
  }

}


