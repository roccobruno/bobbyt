package model

import org.specs2.matcher.Matchers
import org.specs2.mutable.Specification

class TimeOfDaySpec extends Specification {

  "a timeOfDay" should {

    "create an object" in  {

      val obj = TimeOfDay(3,45)
      obj.hour must equalTo(3)
      obj.min must equalTo(45)
    }

    "create new object from existing one" in  {

      val obj = TimeOfDay(3,45).plusMinutes(20)
      obj.hour must equalTo(4)
      obj.min must equalTo(5)
    }

    "throw exception in case of wrong hour" in {

      def obj = TimeOfDay(25,45).plusMinutes(20)
      obj must throwA[IllegalArgumentException]
    }

    "throw exception in case of wrong mins" in {

      def obj = TimeOfDay(25,77).plusMinutes(20)
      obj must throwA[IllegalArgumentException]
    }

    "throw exception in case of wrong negative hour" in {

      def obj = TimeOfDay(-2,45)
      obj must throwA[IllegalArgumentException]
    }

    "throw exception in case of wrong negative mins" in {

      def obj = TimeOfDay(25,-3)
      obj must throwA[IllegalArgumentException]
    }
  }


}
