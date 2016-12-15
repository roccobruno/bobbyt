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




  def checkTubeLinesAndCreatesAlert(): Future[Seq[Option[String]]] = {

    def createAlerts(seq: Seq[Job]): Future[Seq[Option[String]]] = {
      Future.sequence( seq map {
        job => {
          val alertToSave = EmailAlert(email = job.alert, persisted = Some(DateTime.now()), sent = None, jobId = job.getId)
          repo.saveAlertIfAbsent(alertToSave)
        }
      })
    }

    for {
      tubelinesIds <- tubeRepository.findAllWithDisruption()
      jobs <- repo.findJobsByTubeLineAndRunningTime(tubelinesIds.map(id => TubeLine(id.id,id.id)))
      res <- createAlerts(jobs)
    } yield res

  }
}
