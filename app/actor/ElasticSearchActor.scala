package actor

import java.net.URL

import actor.ESQueries.{ESSearchTerm, ESSearchQuery}
import actor.ElasticSearchActor.{InsertStations, SearchForStation}
import akka.actor.Actor.Receive
import akka.actor.{Props, Actor}
import api.Station
import com.netaporter.uri.Uri
import play.api.libs.ws.WS
import play.api.libs.json.{Writes, JsArray, JsValue, Json}
import com.netaporter.uri.dsl._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util._

object ElasticSearchActor {
  def props = Props[ElasticSearchActor]

  case class SearchForStation(stationQuery: String)
  case class InsertStations(stations: Seq[Station])
}

object ESQueries {
  case class ESSearchQuery(queryString: String)

  implicit val esSearchWrites: Writes[ESSearchQuery] = new Writes[ESSearchQuery] {
    override def writes(searchQuery: ESSearchQuery): JsValue = {
      Json.obj(
        "query" -> Json.obj(
          "query_string" -> Json.obj(
            "query" -> searchQuery.queryString
          )
        )
      )
    }
  }

  case class ESSearchTerm(sets: Map[String, String])

  implicit val esSearchTermWrites: Writes[ESSearchTerm] = new Writes[ESSearchTerm] {
    override def writes(searchQuery: ESSearchTerm): JsValue = {
      Json.obj(
        "query" -> Json.obj(
          "constant_score" -> Json.obj(
            "filter" ->
              Json.obj("term" -> Json.toJson(searchQuery.sets))
          )
        )
      )
    }
  }

}

class ElasticSearchActor extends Actor {
  import play.api.Play.current

  val elasticSearchHost = Uri.parse(new URL("http", "server.local", 9200, "").toString)

  override def receive = {
    case InsertStations(stations) =>
      val promises = stations.map { station =>
        val pr = WS.url(elasticSearchHost / "stations" / "station" / station.code).put(Json.stringify(Json.toJson(station)))
        pr.onComplete {
          case Success(s) =>
            println(s.body)

          case Failure(e) =>
            println(e)
        }
        pr
      }


      sender() ! Future.sequence(promises)

    case SearchForStation(stationQuery) =>

      val query = ESSearchQuery(stationQuery)//ESSearchTerm(Map("name" -> stationQuery))

      sender() ! WS.url(elasticSearchHost / "stations" / "_search")
        .post(Json.toJson(query))
        .onSuccess { case result =>

          println(Json.stringify(Json.toJson(query)))
          println(result.body)
        }
  }
}
