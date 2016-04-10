package service.tfl

import model._
import org.joda.time.DateTime
import org.reactivecouchbase.client.OpResult
import repository.JackRepository

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait JobService extends TubeService with TubeConnector {

  val repo: JackRepository

  def findAndProcessActiveJobs(): Future[Seq[RunningJob]] = {

    def process(jobs: Seq[RunningJob], func:(RunningJob) =>Future[Either[String,Any]] ) = {
      Future.sequence(jobs map(func)) map {
        rs =>
          rs.collect {
            case Left(id) => jobs.find(j => j.getId == id)
          }.flatten
      }
    }

    def saveUpdateRunningJob(job: RunningJob):Future[Either[String,Any]] = {
      repo.saveARunningJackJob(job.copy(alertSent = true))
    }

    for {
     jobs <- repo.findRunningJobToExecute()
     processed <- process(jobs,processJob)
     updated <- process(jobs,saveUpdateRunningJob)
    } yield processed
  }

  def processJob(jobToProcess: RunningJob):  Future[Either[String,Any]]= {

    def processLine(lines: Seq[TFLTubeService], job: Job): Future[Option[EmailAlert]] = {

      val allDisr: Seq[Seq[Disruption]] = lines.map { line =>
        line.lineStatuses collect {
          case LineStatus(_, _, _, _, Some(disruption)) => disruption
        }
      }

      if(allDisr.flatten.nonEmpty) {
         Future.successful(Some(EmailAlert(email= job.alert, Some(DateTime.now()),None,job.getId)))
      }
      else {
         Future.successful(None)
      }
    }

    val alert: Future[Option[EmailAlert]] = for {
      job <- repo.findById(jobToProcess.jobId)
      tubeLines <- findTubeByIds(job.get.journey.meansOfTransportation.tubeLines map (_.id))
      alert <- processLine(tubeLines.flatten,job.get)

    } yield alert

    alert flatMap  {
      case Some(al) =>  repo.saveAlert(al) map {
        case Left(_) => Left(jobToProcess.getId)
        case _ => Right("")
      }
      case None => Future.successful(Right(""))
    }
  }
}
