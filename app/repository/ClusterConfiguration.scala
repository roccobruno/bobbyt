package repository

import javax.inject.{Inject, Singleton}

import com.couchbase.client.java.CouchbaseCluster
import play.api.Configuration


@Singleton
class ClusterConfiguration @Inject()(conf: Configuration)  {
  private var _cluster: Option[CouchbaseCluster] = None
  def cluster  = {

    if(_cluster.isEmpty) {
      _cluster = Some(CouchbaseCluster.create(conf.getString("couchbase-host").getOrElse("localhost")))
    }


    _cluster.getOrElse(throw new IllegalArgumentException("I could not create a clutser"))


  }
}
