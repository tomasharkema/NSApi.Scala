package api

import java.net.URL

import com.netaporter.uri.Uri
import play.api.cache.Cache
import scala.concurrent.Future
import scala.xml._
import com.netaporter.uri.dsl._
import play.api.libs.ws.{WSAuthScheme, WS}
import scalaj.http.{HttpRequest, HttpResponse, Http}

import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by tomas on 10-06-15.
 */
object NSApi {
  val StationsUrl = "ns-api-stations-v2"
  val TreinPlannerUrl = "ns-api-treinplanner"

  def endpoint = Uri.parse(new URL("http", "webservices.ns.nl", 80, "").toString)

  def url(path: String) = endpoint / path

  def nsRequest(path: String) = WS
    .url(url(path))
    .withAuth(Auth.username, Auth.password, WSAuthScheme.BASIC)

  private def parse(res: HttpResponse[String]) = XML.loadString(res.body)

  def stations: Future[Seq[Station]] =
    nsRequest(StationsUrl).get().map { response =>
      (response.xml \\ "Station").map(Station.parseStation)
    }

  def advices(from: String = "KBW", to: String = "Rotterdam Centraal"): Future[Seq[Advice]] = {
    nsRequest(TreinPlannerUrl).withQueryString("fromStation" -> from, "toStation" -> to).get().map{ response =>
      (response.xml \\ "ReisMogelijkheid").map(Advice.parseAdvice(_, from, to))
    }
  }

  def advicesFuture(from: String = "KBW", to: String = "Rotterdam Centraal") =
    advices(from, to).map(_.filter(_.vertrek.actual.isAfterNow).filter(_.status != "NIET-MOGELIJK"))

  def adviceFirstPossible(from: String = "KBW", to: String = "Rotterdam Centraal") =
    advicesFuture(from, to).map(_.headOption)
}
