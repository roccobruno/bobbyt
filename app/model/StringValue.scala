package model


object StringValue {
  import scala.language.implicitConversions
  implicit def stringValueToString(e: StringValue): String = e.value
}

trait StringValue {
  def value: String
  override def toString: String = value
}