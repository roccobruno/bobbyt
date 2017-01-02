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
      Play.current.configuration.getString("auth0.clientSecret").get,                  // The key to validate the signature against
      Algorithm.HS256,           // The algorithm we require
      Set(Typ),                  // The set of headers we require (excluding alg)
      Set(Iss),                  // The set of claims we require
      iss = Some(Iss("https://roccobruno.eu.auth0.com/"))  // The iss claim to require (similar optional arguments exist for all registered claims)
    ) match {
      case s:Success[Jwt] => Right(JwtToken(
        s.value.getClaim[Iss].get.value,
        s.value.getClaim[Sub].get.value,
        s.value.getClaim[Aud].get.value.right.get(0),
          new DateTime(s.value.getClaim[Exp].get.value),
        new DateTime(s.value.getClaim[Iat].get.value)))
      case Failure(e) => Logger.info(s"received not valid token $jwt. Error: ${e.getMessage}"); Left(s"Error -  ${e.getMessage}")
    }


  }
}

case class JwtToken(iss: String,sub :String, aud: String, exp: DateTime, iat: DateTime)