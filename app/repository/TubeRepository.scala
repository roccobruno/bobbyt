package repository

import com.couchbase.client.java.{AsyncBucket, CouchbaseCluster}
import model.TFLTubeService
import org.asyncouchbase.bucket.BucketApi
import org.asyncouchbase.index.IndexApi
import org.asyncouchbase.model.OpsResult
import org.asyncouchbase.query.ExpressionImplicits._
import org.asyncouchbase.query.{ANY, SELECT}
import org.joda.time.DateTime
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


object TubeRepository extends TubeRepository {



  val cluster = ClusterConfiguration.cluster
  val bucket = new IndexApi {
    override def asyncBucket: AsyncBucket = cluster.openBucket("tube").async()
  }

  bucket.createPrimaryIndex(deferBuild = false) map {
    _ => Logger.info("PRIMARY INDEX CREATED")
  } recover {
    case ed: Throwable => Logger.error(s"PRIMARY INDEX NOT CREATED error= ${ed.getMessage}")
  }

}

trait TubeRepository  {

  def cluster:CouchbaseCluster

  implicit val validateQuery = false

  def bucket: BucketApi

  def findById(id: String) = {
    bucket.get[TFLTubeService](id)
  }

  def findAllWithDisruption(): Future[Seq[ID]] = {

    val query  = SELECT("id") FROM "tube" WHERE (ANY("line") IN "lineStatuses" SATISFIES ("line.disruption" IS_NOT_NULL ))
    bucket.find[ID](query)

  }

  def saveTubeService(seq: Seq[TFLTubeService]): Future[Seq[OpsResult]] = {
    Future.sequence(seq.map(service => bucket.upsert[TFLTubeService](service.id,service.copy(lastUpdated = Some(DateTime.now)))))
  }

  def deleteById(tubeLineId: String): Future[Any] = {
    bucket.delete[TFLTubeService](tubeLineId)
  }

}


