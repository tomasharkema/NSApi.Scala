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

  def searchStations(query: String, stations: Seq[Station]) = {
    val smallQuery = query.toLowerCase

    stations.map{ station =>
      val equalIndex = station.names.lang.equalsIgnoreCase(query) ||
        station.names.middel.equalsIgnoreCase(query) ||
        station.names.kort.equalsIgnoreCase(query) ||
        station.synoniemen.exists(_.equalsIgnoreCase(query)) ||
        station.code.equalsIgnoreCase(query)

      val index = if (station.code.equals(query) || equalIndex)
        if (station.name.equals(query)) 1000.0 else 100.0
      else {
        val containingIndex = if(station.names.lang.toLowerCase.contains(query.toLowerCase) ||
          station.names.middel.toLowerCase.contains(query.toLowerCase) ||
          station.names.kort.toLowerCase.contains(query.toLowerCase) ||
          station.synoniemen.exists(_.toLowerCase.contains(query.toLowerCase)) ||
          station.code.toLowerCase.contains(query.toLowerCase)) 100.0 else .0

        average(Seq(
          Seq(containingIndex),
          Seq(SearchUtils.similarity(smallQuery, station.names.lang.toLowerCase)),
          Seq(SearchUtils.similarity(smallQuery, station.names.middel.toLowerCase)),
          Seq(SearchUtils.similarity(smallQuery, station.names.kort.toLowerCase)),
          station.synoniemen.map(synoniem => SearchUtils.similarity(smallQuery, synoniem.toLowerCase)).filter(_ != 0)
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
