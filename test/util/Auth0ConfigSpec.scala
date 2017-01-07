package util

import helpers.JwtToken
import io.igl.jwt._
import org.joda.time.DateTime
import play.api.{Logger, Play}

import scala.util.{Failure, Success}

class Auth0ConfigSpec extends Testing  {

  trait Setup extends TokenUtil {




    def decode = {
      DecodedJwt.validateEncodedJwt(
        token, // An encoded jwt as a string
        "secret", // The key to validate the signature against
        Algorithm.HS256, // The algorithm we require
        Set(Typ), // The set of headers we require (excluding alg)
        Set(Iss, Sub, Aud, Exp, Iat) // The set of claims we require
      ) match {
        case s: Success[Jwt] => Right(JwtToken(
          s.value.getClaim[Iss].get.value,
          s.value.getClaim[Sub].get.value,
          s.value.getClaim[Aud].get.value.left.get,
          new DateTime(s.value.getClaim[Exp].get.value),
          new DateTime(s.value.getClaim[Iat].get.value),
          token))
        case Failure(e) => Logger.info(s"received not valid token $token. Error: ${e.getMessage}"); Left(s"Error -  ${e.getMessage}")
      }
    }


  }

  "a auth0 config " should {

    "return Right with valid token" in new Setup {
      decode shouldBe Right(JwtToken("readme", "test", "test",
        new DateTime(expiry), new DateTime(created), token))

    }

    "return left with message in case of expired token" in new Setup {
      override val expiry = DateTime.now().minusHours(1).getMillis / 1000
      decode shouldBe Left("Error -  Jwt has expired")

    }

    "return left with message in case of not valid token, signed with wrong secret" in new Setup {
      override lazy val secret = "wrong-sercret"
      decode shouldBe Left("Error -  Signature is incorrect")

    }


  }

}
