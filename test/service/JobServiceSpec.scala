package service

import java.util.UUID

import _root_.util.Testing
import model._
import org.joda.time.DateTime
import org.mockito.Matchers.any
import org.mockito.{ Mockito}
import org.mockito.Mockito._
import org.scalatest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, OptionValues, WordSpecLike}
import play.api.Configuration
import play.api.libs.ws.WSClient
import repository.{BobbitRepository, TubeRepository}
import service.JobService
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.{Await, Promise, Future}
import org.scalatest.Matchers._
import org.mockito.Matchers.{eq => meq, _}
class JobServiceSpec extends  Testing {

  trait Setup {


     val repoMock = mock(classOf[BobbitRepository])
     val tubeMock = mock(classOf[TubeRepository])

     val service = new JobService {
      override val repo: BobbitRepository = repoMock
      override val ws: WSClient = mock(classOf[WSClient])
      override val tubeRepository: TubeRepository = tubeMock

      override def apiId: String = ""

      override def apiKey: String = ""

       override val configuration: Configuration = mock(classOf[Configuration])

       override def mailGunApiKey: String = ""

       override def mailGunHost: String = ""
     }

  }

  "a service" should {


    "create an alert" in  new Setup {

      val job = Job(alert = Email(EmailAddress("from@mss.it"), EmailAddress("from@mss.it")), journey = Journey(true,
        MeansOfTransportation(Seq(TubeLine("central", "central")), Nil), TimeOfDay(8, 30), 40))

      val lineStatus = LineStatus(10, "", None, Nil, Some(Disruption("minor-delay", "", "", None, None)))
      val tubeService = TFLTubeService("central", "central", Seq(lineStatus))

      val runningJob = RunningJob(from = TimeOfDay(8, 30), to = TimeOfDay(9, 10), jobId = job.getId)

      when(repoMock.findRunningJobToExecute()).thenReturn(Future.successful(Seq(runningJob)))
      when(tubeMock.findById("central")).thenReturn(Future.successful(Some(tubeService)))
      when(repoMock.findById(job.getId)).thenReturn(Future.successful(Some(job)))
      when(repoMock.saveAlert(any[EmailAlert])).thenReturn(Future.successful(Left("id")))
      when(repoMock.saveRunningJob(any[RunningJob])).thenReturn(Future.successful(Left("id")))

      await(service.findAndProcessActiveJobs())

      verify(repoMock, times(1)).saveAlert(any[EmailAlert])
      verify(repoMock, times(1)).saveRunningJob(runningJob.copy(alertSent = true))

    }


    "create no alert in case of no disruption" in new Setup {

      val job = Job(alert = Email(EmailAddress("from@mss.it"), EmailAddress("from@mss.it")), journey = Journey(true,
        MeansOfTransportation(Seq(TubeLine("central", "central")), Nil), TimeOfDay(8, 30), 40))

      val lineStatus = LineStatus(10, "", None, Nil, None)
      val tubeService = TFLTubeService("central", "central", Seq(lineStatus))

      val runningJob = RunningJob(from = TimeOfDay(8, 30), to = TimeOfDay(9, 10), jobId = job.getId)

      when(repoMock.findRunningJobToExecute()).thenReturn(Future.successful(Seq(runningJob)))
      when(tubeMock.findById("central")).thenReturn(Future.successful(Some(tubeService)))
      when(repoMock.findById(job.getId)).thenReturn(Future.successful(Some(job)))
      when(repoMock.saveAlert(any[EmailAlert])).thenReturn(Future.successful(Left("id")))
      when(repoMock.saveRunningJob(any[RunningJob])).thenReturn(Future.successful(Left("id")))

      await(service.findAndProcessActiveJobs())

      verify(repoMock, times(0)).saveAlert(any[EmailAlert])
    }


    "create one alert when multiple disruption for same journey " in  new Setup {

      val job = Job(alert = Email(EmailAddress("from@mss.it"), EmailAddress("from@mss.it")), journey = Journey(true,
        MeansOfTransportation(Seq(TubeLine("central", "central")), Nil), TimeOfDay(8, 30), 40))

      val lineStatus = LineStatus(10, "", None, Nil, Some(Disruption("minor-delay", "", "", None, None)))
      val tubeService = TFLTubeService("central", "central", Seq(lineStatus))
      val tubeService2 = TFLTubeService("northern", "northern", Seq(lineStatus))

      val runningJob = RunningJob(from = TimeOfDay(8, 30), to = TimeOfDay(9, 10), jobId = job.getId)

      when(repoMock.findRunningJobToExecute()).thenReturn(Future.successful(Seq(runningJob)))
      when(tubeMock.findById("central")).thenReturn(Future.successful(Some(tubeService)))
      when(tubeMock.findById("northern")).thenReturn(Future.successful(Some(tubeService2)))
      when(repoMock.findById(job.getId)).thenReturn(Future.successful(Some(job)))
      when(repoMock.saveAlert(any[EmailAlert])).thenReturn(Future.successful(Left("id")))
      when(repoMock.saveRunningJob(any[RunningJob])).thenReturn(Future.successful(Left("id")))

      await(service.findAndProcessActiveJobs())

      verify(repoMock, times(1)).saveAlert(any[EmailAlert])
      verify(repoMock, times(1)).saveRunningJob(runningJob.copy(alertSent = true))

    }


    "create two alert when multiple disruption for two journeys " in  new Setup {

      val job = Job(alert = Email(EmailAddress("from@mss.it"), EmailAddress("from@mss.it")), journey = Journey(true,
        MeansOfTransportation(Seq(TubeLine("central", "central")), Nil), TimeOfDay(8, 30), 40))

      val lineStatus = LineStatus(10, "", None, Nil, Some(Disruption("minor-delay", "", "", None, None)))
      val tubeService = TFLTubeService("central", "central", Seq(lineStatus))
      val tubeService2 = TFLTubeService("northern", "northern", Seq(lineStatus))

      val runningJob = RunningJob(from = TimeOfDay(8, 30), to = TimeOfDay(9, 10), jobId = job.getId)
      val runningJob2 = RunningJob(from = TimeOfDay(8, 30), to = TimeOfDay(9, 10), jobId = job.getId)

      when(repoMock.findRunningJobToExecute()).thenReturn(Future.successful(Seq(runningJob,runningJob2)))
      when(tubeMock.findById("central")).thenReturn(Future.successful(Some(tubeService)))
      when(tubeMock.findById("northern")).thenReturn(Future.successful(Some(tubeService2)))
      when(repoMock.findById(job.getId)).thenReturn(Future.successful(Some(job)))
      when(repoMock.saveAlert(any[EmailAlert])).thenReturn(Future.successful(Left("id")))
      when(repoMock.saveRunningJob(any[RunningJob])).thenReturn(Future.successful(Left("id")))

      val res = await(service.findAndProcessActiveJobs())
      res.size should be(2)
      res contains(runningJob) should be(true)
      res contains(runningJob2) should be(true)

      verify(repoMock, times(2)).saveAlert(any[EmailAlert])
      verify(repoMock, times(1)).saveRunningJob(runningJob.copy(alertSent = true))
      verify(repoMock, times(1)).saveRunningJob(runningJob2.copy(alertSent = true))

    }

    "create an alert and update the running job state" in  new Setup {

      val job = Job(alert = Email(EmailAddress("from@mss.it"), EmailAddress("from@mss.it")), journey = Journey(true,
        MeansOfTransportation(Seq(TubeLine("central", "central")), Nil), TimeOfDay(8, 30), 40))

      val lineStatus = LineStatus(10, "", None, Nil, Some(Disruption("minor-delay", "", "", None, None)))
      val tubeService = TFLTubeService("central", "central", Seq(lineStatus))

      val runningJob = RunningJob(from = TimeOfDay(8, 30), to = TimeOfDay(9, 10), jobId = job.getId)

      when(repoMock.findRunningJobToExecute()).thenReturn(Future.successful(Seq(runningJob)))
      when(tubeMock.findById("central")).thenReturn(Future.successful(Some(tubeService)))
      when(repoMock.findById(job.getId)).thenReturn(Future.successful(Some(job)))
      when(repoMock.saveAlert(any[EmailAlert])).thenReturn(Future.successful(Left("id")))
      when(repoMock.saveRunningJob(any[RunningJob])).thenReturn(Future.successful(Left("id")))

      await(service.findAndProcessActiveJobs())

      verify(repoMock, times(1)).saveAlert(any[EmailAlert])
      verify(repoMock, times(1)).saveRunningJob(runningJob.copy(alertSent = true))

    }
  }

}
