package helpers

import javax.inject.{Inject, Singleton}

import io.igl.jwt.{Jwt, _}
import org.joda.time.DateTime
import play.api.{Configuration, Logger}

import scala.util.{Failure, Success}

@Singleton
class Auth0Config @Inject()(configuration: Configuration) {


  private val secret = configuration.getString("auth0.clientSecret").getOrElse(throw new IllegalStateException("auth0.clientSecret  is missing"))
  private val clientId = configuration.getString("auth0.clientId").getOrElse(throw new IllegalStateException("auth0.clientId  is missing"))
  private val callbackURL = configuration.getString("auth0.callbackURL").getOrElse(throw new IllegalStateException("auth0.callbackURL  is missing"))
  private val domain = configuration.getString("auth0.domain").getOrElse(throw new IllegalStateException("auth0.domain  is missing"))


  def _secret = secret
  def _clientId = clientId
  def _callbackURL = callbackURL
  def _domain = domain


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