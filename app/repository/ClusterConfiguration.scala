package repository

import javax.inject.{Inject, Singleton}

import com.couchbase.client.java.CouchbaseCluster
import play.api.{Configuration, Play}




object ClusterConfiguration  {

  def cluster  = {

    CouchbaseCluster.create("localhost") //TODO read from config


  }


}
