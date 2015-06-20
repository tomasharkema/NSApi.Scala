package settings

import play.api.Play

/**
 * Created by tomas on 20-06-15.
 */
object Settings {
  val ApnsCertUrl = sys.env.getOrElse("APNS_PUSH_CERT", Play.current.configuration.getString("apns.cert").getOrElse(""))
  val ApnsCertPass = sys.env.getOrElse("APNS_PUSH_PASS", Play.current.configuration.getString("apns.pass").getOrElse(""))
  val ApnsCertLocation = "apns-cert.p12"
  val Environment = sys.env.getOrElse("ENVIRONMENT", Play.current.configuration.getString("env.environment").getOrElse(""))
}
