import _root_.actor.PushActor
import akka.actor

import scala.annotation.implicitNotFound
import scala.concurrent.duration.DurationInt
import akka.actor.Props.apply
import play.api.Application
import play.api.GlobalSettings
import play.api.Logger
import play.api.Play
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.concurrent.Akka
import akka.actor.Props

/**
 * Created by tomas on 14-06-15.
 */
object Global extends GlobalSettings {
  override def onStart(app: Application) = {
    val controllerPath = controllers.routes.Ping.ping.url
    play.api.Play.mode(app) match {
      case play.api.Mode.Test => // do not schedule anything for Test
      case _ => pushDaemon(app)
    }
  }

  def pushDaemon(app: Application) = {
    Logger.info("Scheduling the reminder daemon")
    val reminderActor = Akka.system(app).actorOf(Props(new PushActor()))
    Akka.system(app).scheduler.schedule(10 seconds, 1 minute, reminderActor, "reminderDaemon")
  }
}
