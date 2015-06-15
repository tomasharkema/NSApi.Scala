package actor

import akka.actor.{Props, ActorLogging, Actor}
import play.api.{Play, Logger}
import play.api.libs.ws.{WSAuthScheme, WS}
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by tomas on 15-06-15.
 */

object NotifyActor {
  def props = Props[NotifyActor]

  case class Email(emailaddress: String, subject: String, message: String)
}

class NotifyActor extends Actor with ActorLogging {
  import NotifyActor._

  private def email(email: String, subject: String, message: String) = {
    val url = sys.env.getOrElse("PROD_EMAIL_ENDPOINT", Play.current.configuration.getString("email.endpoint").getOrElse(""))
    val user = sys.env.getOrElse("PROD_EMAIL_USER", Play.current.configuration.getString("email.user").getOrElse(""))
    val pass = sys.env.getOrElse("PROD_EMAIL_PASS", Play.current.configuration.getString("email.pass").getOrElse(""))

    Logger.info("Email to " + url + " " + user + ":" + pass + " " + email + " " + subject)

    WS.url(url)
      .withAuth(user, pass, WSAuthScheme.BASIC)
      .post(Map("to" -> Seq(email), "from" -> Seq("tomas@harkema.in"), "subject" -> Seq(subject), "text" -> Seq(message)))
  }

  def receive = {
    case Email(emailaddress, subject, message) =>
      Logger.info("Notify user " + emailaddress + " " + subject + " " + message)
      email(emailaddress, subject, message).map(res => Logger.info("Email to " + emailaddress + " " + res))
  }
}
