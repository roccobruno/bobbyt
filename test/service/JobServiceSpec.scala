package service

import _root_.util.Testing
import model._
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito
import org.mockito.Mockito._
import play.api.Configuration
import play.api.libs.ws.WSClient
import repository.{BobbytRepository, TubeRepository}
import service.tfl.TubeService

import scala.concurrent.Future
class JobServiceSpec extends  Testing {

  trait Setup {


     val repoMock = mock(classOf[BobbytRepository])
     val tubeMock = mock(classOf[TubeRepository])
     val mailGunMock = mock(classOf[MailGunService])

    val jbService =  new JobService (
      Mockito.mock(classOf[Configuration]),
      bobbytRepository,
      tubeMock,
      mailGunMock,
      Mockito.mock(classOf[TubeService])
    )

  }

  "a service" should {








  }

}
