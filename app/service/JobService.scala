package service

import model._
import org.joda.time.DateTime
import repository.BobbitRepository
import service.tfl.{TubeConnector, TubeService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait JobService extends TubeService with TubeConnector  {

  val repo: BobbitRepository
  val mailGunService: MailGunService
  val ALERT_SENT = true
  val ALERT_NOT_SENT = false


  private def process(jobs: Seq[RunningJob], func: (RunningJob) => Future[Option[String]]) = {
    Future.sequence(jobs map (func)) map { rs =>
        rs.collect {
          case Some(id) => jobs.find(j => j.getId == id)
        }.flatten
    }
  }

  private def saveUpdateRunningJob(update: Boolean)(job: RunningJob): Future[Option[String]] = {
    repo.saveRunningJob(job.copy(alertSent = update))
  }

  def findAndProcessActiveJobs(): Future[Seq[RunningJob]] = {
    for {
      jobs <- repo.findRunningJobToExecute()
      processed <- process(jobs.toSeq, processJob)
      updated <- process(jobs.toSeq, saveUpdateRunningJob(ALERT_SENT))
    } yield processed
  }

  def processAlerts(): Future[Seq[String]] = {
    for {
      alerts <- repo.findAllAlert()
      emails <- sendAlert(alerts)
      _ <- deleteAlerts(alerts)
    } yield alerts map (_.jobId)
  }

  def sendAlert(alerts: Seq[EmailAlert]): Future[Seq[MailgunSendResponse]] = {
    def from(alert : EmailAlert) = s"${alert.email.nameFrom} <${alert.email.from.value}>"
    val result = alerts map (al => EmailToSent(from(al), EmailAddress(al.email.to), "",
      Some("Delays"),Some(mailGunService.newTemplate(al.email.nameFrom,al.email.nameTo,al.email.from.value))))
    Future.sequence(result map (mailGunService.sendEmail))

  }

  def deleteAlerts(alerts: Seq[EmailAlert]) = {
    Future.sequence(alerts map (al => repo.deleteById(al.getId)))
  }

  def findAndResetActiveJobs() = {
    for {
      jobs <- repo.findRunningJobToReset()
      updated <- process(jobs, saveUpdateRunningJob(ALERT_NOT_SENT))
    } yield updated
  }


  def processJob(jobToProcess: RunningJob): Future[Option[String]] = {
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
      job <- repo.findJobById(jobToProcess.jobId)
      tubeLines <- findTubeByIds(job.get.journey.meansOfTransportation.tubeLines map (_.id))
      alert <- processLine(tubeLines.flatten, job.get)

    } yield alert

    alert flatMap {
      case Some(al) => repo.saveAlert(al) map {
        case Some(_) => Some(jobToProcess.getId)
        case _ => None
      }
      case None => Future.successful(None)
    }
  }
}
