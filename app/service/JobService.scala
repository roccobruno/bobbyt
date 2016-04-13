package service

import model._
import org.joda.time.DateTime
import repository.BobbitRepository
import service.tfl.{TubeConnector, TubeService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait JobService extends TubeService with TubeConnector {

  val repo: BobbitRepository

  val ALERT_SENT = true
  val ALERT_NOT_SENT = false


  private def process(jobs: Seq[RunningJob], func: (RunningJob) => Future[Either[String, Any]]) = {
    Future.sequence(jobs map (func)) map { rs =>
        rs.collect {
          case Left(id) => jobs.find(j => j.getId == id)
        }.flatten
    }
  }

  private def saveUpdateRunningJob(update: Boolean)(job: RunningJob): Future[Either[String, Any]] = {
    repo.saveRunningJob(job.copy(alertSent = update))
  }

  def findAndProcessActiveJobs(): Future[Seq[RunningJob]] = {
    for {
      jobs <- repo.findRunningJobToExecute()
      processed <- process(jobs, processJob)
      updated <- process(jobs, saveUpdateRunningJob(ALERT_SENT))
    } yield processed
  }

  def processAlerts(): Future[Seq[String]] = {
    for {
      alerts <- repo.findAllAlert()
      emails <- sendEmail(alerts)
      _ <- deleteAlerts(alerts)
    } yield alerts map (_.jobId)
  }

  def sendEmail(alerts: Seq[EmailAlert]) = {
    //TODO send email
    val result = alerts map (al => EmailToSent(al.email.from, EmailAddress(al.email.to), "BODY",None,None))
    println(s"EMAIL SENT : $result")
    Future.successful(result)
  }

  def deleteAlerts(alerts: Seq[EmailAlert]) = {
    Future.sequence(alerts map (al => repo.deleteAlertById(al.getId)))
  }

  def findAndResetActiveJobs() = {
    for {
      jobs <- repo.findRunningJobToReset()
      updated <- process(jobs, saveUpdateRunningJob(ALERT_NOT_SENT))
    } yield updated
  }

  def processJob(jobToProcess: RunningJob): Future[Either[String, Any]] = {

    def processLine(lines: Seq[TFLTubeService], job: Job): Future[Option[EmailAlert]] = {

      val allDisr: Seq[Seq[Disruption]] = lines.map { line =>
        line.lineStatuses collect {
          case LineStatus(_, _, _, _, Some(disruption)) => disruption
        }
      }

      if (allDisr.flatten.nonEmpty) {
        Future.successful(Some(EmailAlert(email = job.alert, persisted =Some(DateTime.now()), sent =None, jobId= job.getId)))
      }
      else {
        Future.successful(None)
      }
    }

    val alert: Future[Option[EmailAlert]] = for {
      job <- repo.findById(jobToProcess.jobId)
      tubeLines <- findTubeByIds(job.get.journey.meansOfTransportation.tubeLines map (_.id))
      alert <- processLine(tubeLines.flatten, job.get)

    } yield alert

    alert flatMap {
      case Some(al) => repo.saveAlert(al) map {
        case Left(_) => Left(jobToProcess.getId)
        case _ => Right("")
      }
      case None => Future.successful(Right(""))
    }
  }
}
