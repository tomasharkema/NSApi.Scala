package actor

import java.net.NetworkInterface

import akka.actor.Status.Failure
import akka.actor.{Props, ActorLogging, Actor}
import akka.util.Timeout
import com.notnoop.apns.APNS
import com.notnoop.exceptions.NetworkIOException
import play.api.{Play, Logger}
import play.api.libs.ws.{WSResponse, WSAuthScheme, WS}
import play.api.Play.current
import settings.Settings
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by tomas on 15-06-15.
 */

object NotifyActor {
  def props = Props[NotifyActor]
  
  case class SendEmailNotification(emailaddress: String, subject: String, message: String)
  case class SendPushNotification(uuid: String, title: String, message: String)
}

object Push {
  val ApnsService = {
    if (Settings.Environment == "PROD") {
      APNS.newService.withCert(Settings.ApnsCertLocation, Settings.ApnsCertPass)
        .withProductionDestination.build
    } else {
      APNS.newService.withCert(Settings.ApnsCertLocation, Settings.ApnsCertPass)
        .withSandboxDestination.build
    }
  }
}

class NotifyActor extends Actor {
  import NotifyActor._

  private def email(email: String, subject: String, message: String): Future[WSResponse] = {
    val url = sys.env.getOrElse("PROD_EMAIL_ENDPOINT", Play.current.configuration.getString("email.endpoint").getOrElse(""))
    val user = sys.env.getOrElse("PROD_EMAIL_USER", Play.current.configuration.getString("email.user").getOrElse(""))
    val pass = sys.env.getOrElse("PROD_EMAIL_PASS", Play.current.configuration.getString("email.pass").getOrElse(""))

    Logger.info("Email to " + url + " " + user + ":" + pass + " " + email + " " + subject)

    WS.url(url)
      .withAuth(user, pass, WSAuthScheme.BASIC)
      .post(Map("to" -> Seq(email), "from" -> Seq("tomas@harkema.in"), "subject" -> Seq(subject), "text" -> Seq(message)))
  }

  @throws(classOf[NetworkIOException])
  private def push(uuid: String, payload: String) = {
    Push.ApnsService.push(uuid, payload)
  }

  def receive = {
    case e: SendEmailNotification =>
      Logger.info("Email user " + e.emailaddress + " " + e.subject + " " + e.message)
      sender() ! email(e.emailaddress, e.subject, e.message).map(res => Logger.info("Email to " + e.emailaddress + " " + res))
    case e: SendPushNotification =>
      Logger.info("Push user " + e.uuid + " " + e.title + " " + e.message)
      try {
        val result = push(e.uuid, "{\"aps\": {\"content-available\":1}, \"content-available\":1, \"info\": {\"message\": \"" + e.message + "\"}}")
        sender() ! result
      } catch {
        case e: Exception =>
          sender() ! Failure(e)
          throw e
      }
    case _ =>
      Logger.error("Unknown Receivant")
  }
}
