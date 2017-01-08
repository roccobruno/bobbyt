package util

import model.TFLTubeService
import play.api.libs.json.Json

trait TubeLineUtil {

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
}
