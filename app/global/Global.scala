package global

import java.io.{File, FileOutputStream}

import _root_.actor.PushActor
import akka.actor.Props
import api.{NSApi, Station}
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee._
import play.api.libs.ws.WS
import play.api.{Application, GlobalSettings, Logger}
import searching.Search
import settings.Settings

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt

/**
 * Created by tomas on 14-06-15.
 */
object Global extends GlobalSettings {

  var stationsCache: Seq[Station] = List()

  override def onStart(app: Application) = {
    val controllerPath = controllers.routes.Ping.ping.url
    play.api.Play.mode(app) match {
      case play.api.Mode.Test => // do not schedule anything for Test
      case _ => pushDaemon(app)
    }

    downloadApns()
    stationsCache = Await.result(NSApi.stations, 10 seconds)
    Search.saveStations(stationsCache)
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
    Akka.system(app).scheduler.schedule(0 seconds, 2 minutes, pushActor, "pushDaemon")
  }
}
