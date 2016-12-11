package model

object Converters {

  def emailToFormBody(email: EmailToSent): Map[String, Seq[String]] = {
    val mandatoryFields = Map(
      "from" -> email.from,
      "to" -> email.to,
      "subject" -> email.subject.getOrElse("TEST"),
      "text" -> email.text,
      "html" -> email.htmlBody.getOrElse("")
    )
    (mandatoryFields).mapValues(Seq(_))
  }

}
