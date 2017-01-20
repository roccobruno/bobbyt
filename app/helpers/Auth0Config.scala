package helpers

import io.igl.jwt.{Jwt, _}
import org.joda.time.DateTime
import play.api.{Logger, Play}

import scala.util.{Failure, Success}

case class Auth0Config(secret: String, clientId: String, callbackURL: String, domain: String)

object Auth0Config {
  def get() = {
    Auth0Config(
          Play.current.configuration.getString("auth0.clientSecret").get,
          Play.current.configuration.getString("auth0.clientId").get,
          Play.current.configuration.getString("auth0.callbackURL").get,
          Play.current.configuration.getString("auth0.domain").get
    )
  }


  def decodeAndVerifyToken(jwt: String): Either[String, JwtToken] = {

    DecodedJwt.validateEncodedJwt(
      jwt,                       // An encoded jwt as a string
      "secret",                  // The key to validate the signature against
      Algorithm.HS256,           // The algorithm we require
      Set(Typ),                  // The set of headers we require (excluding alg)
      Set(Iss, Sub, Aud, Exp, Iat)                  // The set of claims we require
    ) match {
      case s:Success[Jwt] => Right(JwtToken(
        s.value.getClaim[Iss].get.value,
        s.value.getClaim[Sub].get.value,
        s.value.getClaim[Aud].get.value.left.get,
        new DateTime(s.value.getClaim[Exp].get.value),
        new DateTime(s.value.getClaim[Iat].get.value),
        jwt))
      case Failure(e) => Logger.info(s"received not valid token $jwt. Error: ${e.getMessage}"); Left(s"Error -  ${e.getMessage}")
    }
  }


}

case class JwtToken(iss: String,sub :String, aud: String, exp: DateTime, iat: DateTime, token: String) {
  def userId = iss // TODO
}

case class EncodedJwtToken(value: String)