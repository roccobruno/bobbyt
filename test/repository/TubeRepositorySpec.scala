package repository

import model.TFLTubeService
import play.api.libs.json.Json
import util.{Testing, TubeLineUtil}

class TubeRepositorySpec extends Testing with TubeLineUtil {


  val tubeRepository = TubeRepository




  "a repository" should {

    "store tube line records" in {
      val tubeLines = await(tubeRepository.findById("piccadilly"))



      //insert record in
      await(tubeRepository.saveTubeService(Seq(tubeLine("testLine"))))

      val loadResult = await(tubeRepository.findById("testLine"))
      loadResult.isDefined shouldBe true

      loadResult.get.id shouldBe "testLine"
      loadResult.get.lineStatuses.size shouldBe 1

      await(tubeRepository.deleteById("testLine"))
      val loadResult2 = await(tubeRepository.findById("testLine"))
      loadResult2.isDefined shouldBe false
    }


    "find all lines with disruption" in {

      //insert record in
      await(tubeRepository.saveTubeService(Seq(tubeLine("testLine"))))
      await(tubeRepository.saveTubeService(Seq(tubeLineNoDisruption("testLineNoDisruption"))))

      val loadResult = await(tubeRepository.findAllWithDisruption())
      loadResult.size shouldBe 1
      await(tubeRepository.deleteById("testLine"))
      await(tubeRepository.deleteById("testLineNoDisruption"))

    }


  }

}
