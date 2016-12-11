package model

import org.joda.time.DateTime
import play.api.libs.json.{Json, Reads}


case class Disruption(category: String, description: String, closureText: String, isBlocking: Option[Boolean], isWholeLine: Option[Boolean])
case class Period(fromDate: DateTime,toDate: DateTime)
case class LineStatus(statusSeverity: Int,statusSeverityDescription: String, reason: Option[String],validityPeriods: Seq[Period],disruption: Option[Disruption])
case class TFLTubeService(name: String, id: String, lineStatuses: Seq[LineStatus], lastUpdated : Option[DateTime] = Some(DateTime.now()))

object Disruption {
  implicit val format = Json.format[Disruption]
}

object Period {
  implicit val dateReads = Reads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ssZ")
  implicit val format = Json.format[Period]
}

object LineStatus {
  implicit val format = Json.format[LineStatus]
}

object TFLTubeService {
  implicit val format = Json.format[TFLTubeService]
}
