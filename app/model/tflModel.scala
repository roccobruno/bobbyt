package model

import org.joda.time.DateTime
import play.api.libs.json.Json


case class Disruption(category: String, description: String, closureText: String, isBlocking: Boolean)
case class Period(fromDate: DateTime,toDate: DateTime)
case class LineStatus(statusSeverity: Int,statusSeverityDescription: String, reason: String,validityPeriods: Seq[Period],disruption: Disruption)
case class TFLTubeService(name: String, id: String, lineStatus: Seq[LineStatus])

object Disruption {
  implicit val format = Json.format[Disruption]
}

object Period {
  implicit val format = Json.format[Period]
}

object LineStatus {
  implicit val format = Json.format[LineStatus]
}

object TFLTubeService {
  implicit val format = Json.format[TFLTubeService]
}
