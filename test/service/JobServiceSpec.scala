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








  }

}
