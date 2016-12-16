package service

import model._
import org.joda.time.DateTime
import repository.{BobbitRepository, ID}
import service.tfl.{TubeConnector, TubeService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait JobService extends TubeService with TubeConnector  {
  def deleteSentAlerts() = {
    Future.successful(repo.deleteAll[ID](repo.findAllAlertSentYesterday))
  }


  val repo: BobbitRepository
  val mailGunService: MailGunService
  val ALERT_SENT = true
  val ALERT_NOT_SENT = false




  def processAlerts(): Future[Seq[String]] = {
    for {
      alerts <- repo.findAllAlertNotSent()
      emails <- sendAlert(alerts)
      _ <- updateAlert(alerts)
      _ <- updateAlertSentDate(alerts)
    } yield alerts map (_.jobId)
  }

  def sendAlert(alerts: Seq[EmailAlert]): Future[Seq[MailgunSendResponse]] = {
    def from(alert : EmailAlert) = s"${alert.email.nameFrom} <${alert.email.from.value}>"
    val result = alerts map (al => EmailToSent(from(al), EmailAddress(al.email.to), "",
      Some("Delays"),Some(mailGunService.newTemplate(al.email.nameFrom,al.email.nameTo,al.email.from.value))))
    Future.sequence(result map (mailGunService.sendEmail))

  }

  def updateAlert(alerts: Seq[EmailAlert]) = {
    Future.sequence(alerts map (al => repo.markAlertAsSent(al.getId)))
  }

  def updateAlertSentDate(alerts: Seq[EmailAlert]) = {
    Future.sequence(alerts map (al => repo.markAlertAsSentAt(al.getId)))
  }




  def checkTubeLinesAndCreatesAlert(): Future[Seq[Option[String]]] = {

    def createAlerts(seq: Seq[Job]): Future[Seq[Option[String]]] = {
      Future.sequence( seq map {
        job => {
          val alertToSave = EmailAlert(email = job.alert, persisted = DateTime.now(), sentAt = None, jobId = job.getId)
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
