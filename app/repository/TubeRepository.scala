package repository

import javax.inject.{Inject, Singleton}

import com.couchbase.client.java.{AsyncBucket, CouchbaseCluster}
import model.TFLTubeService
import org.asyncouchbase.bucket.BucketApi
import org.asyncouchbase.index.IndexApi
import org.asyncouchbase.model.OpsResult
import org.asyncouchbase.query.ExpressionImplicits._
import org.asyncouchbase.query.{ANY, SELECT}
import org.joda.time.DateTime
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class TubeRepository @Inject()(clusterConfiguration: ClusterConfiguration)   {

  val cluster = clusterConfiguration.cluster

  val BUCKET_NAME = "tube"

  val bucket = new IndexApi {
    override def asyncBucket: AsyncBucket = cluster.openBucket(BUCKET_NAME).async()
  }

  bucket.createPrimaryIndex(deferBuild = false) map {
    _ => Logger.info("PRIMARY INDEX CREATED")
  } recover {
    case ed: Throwable => Logger.error(s"PRIMARY INDEX NOT CREATED error= ${ed.getMessage}")
  }


  implicit val validateQuery = false

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

  def deleteAllTubeLines() = {

    val query =  SELECT ("*") FROM BUCKET_NAME
    bucket.find[TFLTubeService](query) map {
      res =>
        res.foreach {
          record => deleteById(record.id)
        }
    }
  }

  def findAllTubeLines() = {
    val query =  SELECT ("*") FROM BUCKET_NAME
    bucket.find[TFLTubeService](query)
  }

}


