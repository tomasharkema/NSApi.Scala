package searching

import actor.SearchActor.StationSearch
import actor.SearchActor.StationLocationSearch
import actor.{SearchActor, NotifyActor}
import akka.actor.Props
import akka.util.Timeout
import api.Station
import play.libs.Akka
import akka.pattern.ask
import akka.util.Timeout
import utils.{LatLngUtils, SearchUtils}
import utils.SeqUtils._
import scala.concurrent.duration.DurationInt
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Search {
  implicit val timeout = Timeout(5 seconds)

  val searchActor = Akka.system.actorOf(Props[SearchActor], name = "search-actor")

  def searchStations(query: String, stations: Seq[Station]): Seq[(Double, Station)] = {
    if (query == "") {
      return Seq()
    }

    val smallQuery = query.toLowerCase

    stations.map{ station =>
      val comparables = Seq(
        Seq(
          station.names.long,
          station.names.middle,
          station.names.short
        ),
        station.synonyms
      ).flatten.map(_.replaceAll("-", " ").toLowerCase)

      val somethingEquals = comparables.map(_.equalsIgnoreCase(query)).exists(b => b)

      val index = if (station.code.equals(query) || somethingEquals)
        if (station.name.equalsIgnoreCase(query)) 1000.0 else 150.0
      else {

        val containingIndex = if(comparables.map(_.contains(query)).exists(b => b)) 100.0 else .0
        val startsWithIndex = if (comparables.map(_.startsWith(smallQuery)).exists(b => b)) 10.0 else 0.0

        average(Seq(
          Seq(
            startsWithIndex,
            containingIndex,
            SearchUtils.similarity(smallQuery, station.names.long.toLowerCase),
            SearchUtils.similarity(smallQuery, station.names.middle.toLowerCase),
            SearchUtils.similarity(smallQuery, station.names.short.toLowerCase)
          ),
          station.synonyms.map(synoniem => SearchUtils.similarity(smallQuery, synoniem.toLowerCase)).filter(_ != 0)
        ).flatten)
      }

      (index, station)
    }.filter(_._1 > 0.1).sortBy(_._1).reverse
  }

  def searchStations(lat: Double, lon: Double, stations: Seq[Station]) = {
    stations.map { station =>
      val distance = LatLngUtils.distFrom(lat, lon, station.coords.lat, station.coords.lon)

      (distance, station)
    }
      .sortBy(_._1).take(20)
  }

  def stations(query: String, stations: Seq[Station]): Future[Seq[(Double, Station)]] = {
    ask(searchActor, StationSearch(query, stations)).map {
      case seq: Seq[(Double, Station)] =>
        seq
      case _ =>
        List()
    }
  }

  def stations(lat: Double, lon: Double, stations: Seq[Station]): Future[Seq[(Float, Station)]] = {
    ask(searchActor, StationLocationSearch(lat, lon, stations)).map {
      case seq: Seq[(Float, Station)] =>
        seq
      case _ =>
        List()
    }
  }
}
