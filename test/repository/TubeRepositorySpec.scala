package repository

import model.TFLTubeService
import play.api.libs.json.Json
import util.Testing

class TubeRepositorySpec extends Testing {


  val tubeRepository = TubeRepository


  def tubeLine(id: String) = {

    val json =
      s"""{
          |  "name": "$id",
          |  "lastUpdated": 1481649466183,
          |  "id": "$id",
          |  "lineStatuses": [
          |    {
          |      "statusSeverityDescription": "Severe Delays",
          |      "statusSeverity": 6,
          |      "reason": "Piccadilly Line: Severe delays while we carry out repairs to our trains. London Underground tickets are being accepted on local buses, Chiltern Railways, Great Western Railway, South West trains, Great Northern and London Overground Customers for stations between Rayners Lane and Uxbridge should use Metropolitan line services. A shuttle train service operates between Acton Town and Rayners  Lane and a reduced bus service operates between Acton Town and Rayners Lane calling at Hanger Lane for the Central line instead of Park Royal. ",
          |      "validityPeriods": [
          |        {
          |          "fromDate": 1481641535000,
          |          "toDate": 1481678940000
          |        }
          |      ],
          |      "disruption": {
          |        "closureText": "severeDelays",
          |        "isWholeLine": true,
          |        "description": "Piccadilly Line: Severe delays while we carry out repairs to our trains. London Underground tickets are being accepted on local buses, Chiltern Railways, Great Western Railway, South West trains, Great Northern and London Overground Customers for stations between Rayners Lane and Uxbridge should use Metropolitan line services. A shuttle train service operates between Acton Town and Rayners  Lane and a reduced bus service operates between Acton Town and Rayners Lane calling at Hanger Lane for the Central line instead of Park Royal. ",
          |        "category": "RealTime"
          |      }
          |    }
          |  ]
          |}""".stripMargin


    Json.fromJson[TFLTubeService](Json.parse(json)).get
  }


  def tubeLineNoDisruption(id: String) = {

    val json =
      s"""{
          |  "name": "$id",
          |  "lastUpdated": 1481649466183,
          |  "id": "$id",
          |  "lineStatuses": [
          |    {
          |      "statusSeverityDescription": "Good Service",
          |      "statusSeverity": 10,
          |      "validityPeriods": []
          |    }
          |  ]
          |}""".stripMargin


    Json.fromJson[TFLTubeService](Json.parse(json)).get
  }


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
      loadResult.size shouldBe 5
      await(tubeRepository.deleteById("testLine"))
      await(tubeRepository.deleteById("testLineNoDisruption"))

    }


  }

}
