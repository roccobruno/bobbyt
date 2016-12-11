package service

import _root_.util.Testing
import model._
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito._
import play.api.Configuration
import play.api.libs.ws.WSClient
import repository.{BobbitRepository, TubeRepository}

import scala.concurrent.Future
class JobServiceSpec extends  Testing {

  trait Setup {


     val repoMock = mock(classOf[BobbitRepository])
     val tubeMock = mock(classOf[TubeRepository])
     val mailGunMock = mock(classOf[MailGunService])

     val service = new JobService {
      override val repo: BobbitRepository = repoMock
      override val ws: WSClient = mock(classOf[WSClient])
      override val tubeRepository: TubeRepository = tubeMock

      override def apiId: String = ""

      override def apiKey: String = ""

       override val configuration: Configuration = mock(classOf[Configuration])
       override val mailGunService: MailGunService = mailGunMock
     }

  }

  "a service" should {


    "create an alert" in  new Setup {

      val job = Job("JbTitle",alert = Email("name",EmailAddress("from@mss.it"),"name", EmailAddress("from@mss.it")), journey = Journey(true,
        MeansOfTransportation(Seq(TubeLine("central", "central")), Nil), TimeOfDay(8, 30), 40),accountId =  "accountID")

      val lineStatus = LineStatus(10, "", None, Nil, Some(Disruption("minor-delay", "", "", None, None)))
      val tubeService = TFLTubeService("central", "central", Seq(lineStatus))

      val runningJob = RunningJob(fromTime = TimeOfDay(8, 30), toTime = TimeOfDay(9, 10), jobId = job.getId)

      when(repoMock.findRunningJobToExecute()).thenReturn(Future.successful(Set(runningJob)))
      when(tubeMock.findById("central")).thenReturn(Future.successful(Some(tubeService)))
      when(repoMock.findJobById(job.getId)).thenReturn(Future.successful(Some(job)))
      when(repoMock.saveAlert(any[EmailAlert])).thenReturn(Future.successful(Some("id")))
      when(repoMock.saveRunningJob(any[RunningJob])).thenReturn(Future.successful(Some("id")))

      await(service.findAndProcessActiveJobs())

      verify(repoMock, times(1)).saveAlert(any[EmailAlert])
      verify(repoMock, times(1)).saveRunningJob(runningJob.copy(alertSent = true))

    }


    "create no alert in case of no disruption" in new Setup {

      val job = Job("JobTitle",alert = Email("name",EmailAddress("from@mss.it"),"name", EmailAddress("from@mss.it")), journey = Journey(true,
        MeansOfTransportation(Seq(TubeLine("central", "central")), Nil), TimeOfDay(8, 30), 40),accountId =  "accountID")

      val lineStatus = LineStatus(10, "", None, Nil, None)
      val tubeService = TFLTubeService("central", "central", Seq(lineStatus))

      val runningJob = RunningJob(fromTime = TimeOfDay(8, 30), toTime = TimeOfDay(9, 10), jobId = job.getId)

      when(repoMock.findRunningJobToExecute()).thenReturn(Future.successful(Set(runningJob)))
      when(tubeMock.findById("central")).thenReturn(Future.successful(Some(tubeService)))
      when(repoMock.findJobById(job.getId)).thenReturn(Future.successful(Some(job)))
      when(repoMock.saveAlert(any[EmailAlert])).thenReturn(Future.successful(Some("id")))
      when(repoMock.saveRunningJob(any[RunningJob])).thenReturn(Future.successful(Some("id")))

      await(service.findAndProcessActiveJobs())

      verify(repoMock, times(0)).saveAlert(any[EmailAlert])
    }


    "create one alert when multiple disruption for same journey " in  new Setup {

      val job = Job("job",alert = Email("name",EmailAddress("from@mss.it"),"name", EmailAddress("from@mss.it")), journey = Journey(true,
        MeansOfTransportation(Seq(TubeLine("central", "central")), Nil), TimeOfDay(8, 30), 40),accountId =  "accountID")

      val lineStatus = LineStatus(10, "", None, Nil, Some(Disruption("minor-delay", "", "", None, None)))
      val tubeService = TFLTubeService("central", "central", Seq(lineStatus))
      val tubeService2 = TFLTubeService("northern", "northern", Seq(lineStatus))

      val runningJob = RunningJob(fromTime = TimeOfDay(8, 30), toTime = TimeOfDay(9, 10), jobId = job.getId)

      when(repoMock.findRunningJobToExecute()).thenReturn(Future.successful(Set(runningJob)))
      when(tubeMock.findById("central")).thenReturn(Future.successful(Some(tubeService)))
      when(tubeMock.findById("northern")).thenReturn(Future.successful(Some(tubeService2)))
      when(repoMock.findJobById(job.getId)).thenReturn(Future.successful(Some(job)))
      when(repoMock.saveAlert(any[EmailAlert])).thenReturn(Future.successful(Some("id")))
      when(repoMock.saveRunningJob(any[RunningJob])).thenReturn(Future.successful(Some("id")))

      await(service.findAndProcessActiveJobs())

      verify(repoMock, times(1)).saveAlert(any[EmailAlert])
      verify(repoMock, times(1)).saveRunningJob(runningJob.copy(alertSent = true))

    }


    "create two alert when multiple disruption for two journeys " in  new Setup {

      val job = Job("JobT",alert = Email("name",EmailAddress("from@mss.it"),"name", EmailAddress("from@mss.it")), journey = Journey(true,
        MeansOfTransportation(Seq(TubeLine("central", "central")), Nil), TimeOfDay(8, 30), 40),accountId =  "accountID")

      val lineStatus = LineStatus(10, "", None, Nil, Some(Disruption("minor-delay", "", "", None, None)))
      val tubeService = TFLTubeService("central", "central", Seq(lineStatus))
      val tubeService2 = TFLTubeService("northern", "northern", Seq(lineStatus))

      val runningJob = RunningJob(fromTime = TimeOfDay(8, 30), toTime = TimeOfDay(9, 10), jobId = job.getId)
      val runningJob2 = RunningJob(fromTime = TimeOfDay(8, 30), toTime = TimeOfDay(9, 10), jobId = job.getId)

      when(repoMock.findRunningJobToExecute()).thenReturn(Future.successful(Set(runningJob,runningJob2)))
      when(tubeMock.findById("central")).thenReturn(Future.successful(Some(tubeService)))
      when(tubeMock.findById("northern")).thenReturn(Future.successful(Some(tubeService2)))
      when(repoMock.findJobById(job.getId)).thenReturn(Future.successful(Some(job)))
      when(repoMock.saveAlert(any[EmailAlert])).thenReturn(Future.successful(Some("id")))
      when(repoMock.saveRunningJob(any[RunningJob])).thenReturn(Future.successful(Some("id")))

      val res = await(service.findAndProcessActiveJobs())
      res.size should be(2)
      res contains(runningJob) should be(true)
      res contains(runningJob2) should be(true)

      verify(repoMock, times(2)).saveAlert(any[EmailAlert])
      verify(repoMock, times(1)).saveRunningJob(runningJob.copy(alertSent = true))
      verify(repoMock, times(1)).saveRunningJob(runningJob2.copy(alertSent = true))

    }

    "create an alert and update the running job state" in  new Setup {

      val job = Job("jobTitlte",alert = Email("name",EmailAddress("from@mss.it"),"name", EmailAddress("from@mss.it")), journey = Journey(true,
        MeansOfTransportation(Seq(TubeLine("central", "central")), Nil), TimeOfDay(8, 30), 40),accountId =  "accountID")

      val lineStatus = LineStatus(10, "", None, Nil, Some(Disruption("minor-delay", "", "", None, None)))
      val tubeService = TFLTubeService("central", "central", Seq(lineStatus))

      val runningJob = RunningJob(fromTime = TimeOfDay(8, 30), toTime = TimeOfDay(9, 10), jobId = job.getId)

      when(repoMock.findRunningJobToExecute()).thenReturn(Future.successful(Set(runningJob)))
      when(tubeMock.findById("central")).thenReturn(Future.successful(Some(tubeService)))
      when(repoMock.findJobById(job.getId)).thenReturn(Future.successful(Some(job)))
      when(repoMock.saveAlert(any[EmailAlert])).thenReturn(Future.successful(Some("id")))
      when(repoMock.saveRunningJob(any[RunningJob])).thenReturn(Future.successful(Some("id")))

      await(service.findAndProcessActiveJobs())

      verify(repoMock, times(1)).saveAlert(any[EmailAlert])
      verify(repoMock, times(1)).saveRunningJob(runningJob.copy(alertSent = true))

    }
  }

}
