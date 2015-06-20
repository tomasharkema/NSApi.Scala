package actor

import java.net.NetworkInterface

import akka.actor.Status.Failure
import akka.actor.{Props, ActorLogging, Actor}
import com.notnoop.apns.APNS
import com.notnoop.exceptions.NetworkIOException
import play.api.{Play, Logger}
import play.api.libs.ws.{WSAuthScheme, WS}
import play.api.Play.current
import settings.Settings
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by tomas on 15-06-15.
 */

object NotifyActor {
  def props = Props[NotifyActor]

  case class EmailNotification(emailaddress: String, subject: String, message: String)
  case class PushNotification(uuid: String, title: String, message: String)
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

  @throws(classOf[NetworkIOException])
  private def push(uuid: String, title: String, message: String) = {
    val payload = APNS.newPayload.alertBody(message).badge(1).sound("default").build()
    Push.ApnsService.push(uuid, payload)
  }

  def receive = {
    case EmailNotification(emailaddress, subject, message) =>
      Logger.info("Notify user " + emailaddress + " " + subject + " " + message)
      email(emailaddress, subject, message).map(res => Logger.info("Email to " + emailaddress + " " + res))
    case PushNotification(uuid, title, message) =>
      Logger.info("Push user " + uuid + " " + title + " " + message)
      try {
        val result = push(uuid, title, message)
        sender() ! result
      } catch {
        case e: Exception =>
          sender() ! Failure(e)
          println(e)
          throw e
      }
  }
}
