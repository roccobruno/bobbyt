package util

import io.igl.jwt._
import org.joda.time.DateTime

trait TokenUtil {

  val expiry: Long = DateTime.now().plusHours(1).getMillis
  val created = DateTime.now().getMillis
  val jwt = new DecodedJwt(Seq(Alg(Algorithm.HS256), Typ("JWT")), Seq(Iss("readme"), Sub("test"), Aud("test"), Exp(expiry), Iat(created)))
  lazy val secret = "secret"

  val token = jwt.encodedAndSigned(secret)

}
