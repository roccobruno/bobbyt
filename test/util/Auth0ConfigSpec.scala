package util

import helpers.JwtToken
import io.igl.jwt._
import org.joda.time.DateTime
import play.api.{Logger, Play}

import scala.util.{Failure, Success}

class Auth0ConfigSpec extends Testing {


  "a auth0 config " should {

    "return Right" in {


  val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL3JvY2NvYnJ1bm8uZXUuYXV0aDAuY29tLyIsInN1YiI6ImZhY2Vib29rfDEwMjExNzE5NDI1MTY5MjU0IiwiYXVkIjoiaUw1a3FCbGc5eTJQT1NrQ0tMNUVvTmNZaVFoMjM0N2kiLCJleHAiOjE0ODM0MjMyNDcsImlhdCI6MTQ4MzM4NzI0N30.YjyyPo5oa5r176x0cJjf6GaQAidoJZpYVJi6i3EuKfU"

     val ers = DecodedJwt.validateEncodedJwt(
        token,                       // An encoded jwt as a string
        "",                  // The key to validate the signature against
        Algorithm.HS256,           // The algorithm we require
       Set(Typ),                  // The set of headers we require (excluding alg)
       Set(Iss, Sub, Aud, Exp, Iat)                  // The set of claims we require
      ) match {
        case s:Success[Jwt] => Right(JwtToken(
          s.value.getClaim[Iss].get.value,
          s.value.getClaim[Sub].get.value,
//          s.value.getClaim[Aud].get.value.right.get(0),
          "", //TODO
          new DateTime(s.value.getClaim[Exp].get.value),
          new DateTime(s.value.getClaim[Iat].get.value)))
        case Failure(e) => Logger.info(s"received not valid token $token. Error: ${e.getMessage}"); Left(s"Error -  ${e.getMessage}")
     }


      /*
      {
  "iss": "https://roccobruno.eu.auth0.com/",
  "sub": "facebook|10211719425169254",
  "aud": "iL5kqBlg9y2POSkCKL5EoNcYiQh2347i",
  "exp": 1483423247,
  "iat": 1483387247
}
       */
   ers shouldBe Right(JwtToken("XXXX","XXXX",
     new DateTime(1483423247), new DateTime(1483387247)))

    }


  }

}
