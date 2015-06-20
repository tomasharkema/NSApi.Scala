import java.io.{File, FileOutputStream}

import _root_.actor.PushActor
import play.api.libs.ws.WS
import settings.Settings
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import play.api.{Play, Application, GlobalSettings, Logger}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.concurrent.Akka
import akka.actor.Props
import play.api.libs.iteratee._
import play.api.Play.current

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
    println("Download APNS File")
    downloadApns().onComplete { file =>
      println("Download APNS File completed")
    }
  }

  private def downloadApns() = {

    val file = new File(Settings.ApnsCertLocation)

    val futureResponse = WS.url(Settings.ApnsCertUrl)
      .getStream()

    val downloadedFile: Future[File] = futureResponse.flatMap {
      case (headers, body) =>
        val outputStream = new FileOutputStream(file)

        // The iteratee that writes to the output stream
        val iteratee = Iteratee.foreach[Array[Byte]] { bytes =>
          outputStream.write(bytes)
        }

        // Feed the body into the iteratee
        (body |>>> iteratee).andThen {
          case result =>
            // Close the output stream whether there was an error or not
            outputStream.close()
            // Get the result or rethrow the error
            result.get
        }.map(_ => file)
    }
    downloadedFile
  }

  def pushDaemon(app: Application) = {
    Logger.info("Scheduling the push daemon")
    val pushActor = Akka.system(app).actorOf(Props(new PushActor()))
    Akka.system(app).scheduler.schedule(0 seconds, 30 seconds, pushActor, "pushDaemon")
  }
}
