package jobs

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.mockito.Mockito.mock
import play.api.Configuration
import play.api.libs.ws.WSClient
import repository.TubeRepository
import service.tfl.{TubeConnector, TubeService}
import service.{JobService, MailGunService}
import util.Testing

class JobsRegistrySpec extends TestKit(ActorSystem("ProcessAlertsJobActorSpec")) with Testing with ImplicitSender  {


  "a registry " should {

      val mockConfig: Configuration = mock(classOf[Configuration])
      val jbService =  new JobService (
        mockConfig,
        bobbytRepository,
        mock(classOf[TubeRepository]),
        mock(classOf[MailGunService]),
        mock(classOf[TubeService])
      )
      val tubeConnector = new TubeConnector(mock(classOf[WSClient]), mockConfig)
      val tubeService =  new TubeService(tubeRepository, tubeConnector)
      val registry = new JobsRegistry(system, tubeService, jbService, mockConfig )



    "start all the jobs " in  {



      registry.startJobs
      registry.alertCleanerJobScheduleJob.isCancelled shouldBe false
      registry.tubeCheckerScheduleJob.isCancelled shouldBe false
      registry.alertJobScheduleJob.isCancelled shouldBe false
      registry.tubeScheduleJob.isCancelled shouldBe false
    }

    "stop all the jobs" in  {

      registry.cancelAllJobs
      registry.alertCleanerJobScheduleJob.isCancelled shouldBe true
      registry.tubeCheckerScheduleJob.isCancelled shouldBe true
      registry.alertJobScheduleJob.isCancelled shouldBe true
      registry.tubeScheduleJob.isCancelled shouldBe true
    }


  }




}
