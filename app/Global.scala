import _root_.actor.PushActor
import scala.concurrent.duration.DurationInt
import play.api.Application
import play.api.GlobalSettings
import play.api.Logger
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
    Logger.info("Scheduling the push daemon")
    val pushActor = Akka.system(app).actorOf(Props(new PushActor()))
    Akka.system(app).scheduler.schedule(0 seconds, 30 seconds, pushActor, "pushDaemon")
  }
}
