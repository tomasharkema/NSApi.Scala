package api

import play.api.Play

/**
 * Created by tomas on 10-06-15.
 */
object Auth {
  def username = sys.env.getOrElse("NS_USER", Play.current.configuration.getString("nsapi.user").getOrElse(""))
  def password = sys.env.getOrElse("NS_PASS", Play.current.configuration.getString("nsapi.pass").getOrElse(""))
}
