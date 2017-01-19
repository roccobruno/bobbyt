package service

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import model._
import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.ws.WSClient
import repository.{BobbytRepository, ID, TubeRepository}
import service.tfl.{TubeConnector, TubeService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class JobService @Inject()(conf: Configuration, bobbytRepository: BobbytRepository, tubeRepository: TubeRepository, mailGunService: MailGunService, tubeService: TubeService)  {



  def deleteSentAlerts() = {
    Future.successful(bobbytRepository.deleteAll[ID](bobbytRepository.findAllAlertSentYesterday))
  }


  val ALERT_SENT = true
  val ALERT_NOT_SENT = false



 //TODO can u merge the 2 update operations?
  def processAlerts(): Future[Seq[String]] = {
    for {
      alerts <- bobbytRepository.findAllAlertNotSent()
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
    Future.sequence(alerts map (al => bobbytRepository.markAlertAsSent(al.getId)))
  }

  def updateAlertSentDate(alerts: Seq[EmailAlert]) = {
    Future.sequence(alerts map (al => bobbytRepository.markAlertAsSentAt(al.getId)))
  }




  def checkTubeLinesAndCreatesAlert(): Future[Seq[Option[String]]] = {

    def createAlerts(seq: Seq[Job]): Future[Seq[Option[String]]] = {
      Future.sequence( seq map {
        job => {
          val alertToSave = EmailAlert(email = job.alert, persisted = DateTime.now(), sentAt = None, jobId = job.getId)
          bobbytRepository.saveAlertIfAbsent(alertToSave)
        }
      })
    }

    for {
      tubelinesIds <- tubeRepository.findAllWithDisruption()
      jobs <- bobbytRepository.findJobsByTubeLineAndRunningTime(tubelinesIds.map(id => TubeLine(id.id,id.id)))
      res <- createAlerts(jobs)
    } yield res

  }
}
