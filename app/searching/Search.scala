package searching

import actor.SearchActor.StationSearch
import actor.{SearchActor, NotifyActor}
import akka.actor.Props
import akka.util.Timeout
import api.Station
import play.libs.Akka
import akka.pattern.ask
import akka.util.Timeout
import utils.SearchUtils
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
      val equalIndex = station.names.lang.toLowerCase.equals(query.toLowerCase) ||
        station.names.middel.toLowerCase.equals(query.toLowerCase) ||
        station.names.kort.toLowerCase.equals(query.toLowerCase) ||
        station.synoniemen.exists(_.toLowerCase.equals(query.toLowerCase)) ||
        station.code.toLowerCase.equals(query.toLowerCase)

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

  def stations(query: String, stations: Seq[Station]): Future[Seq[(Double, Station)]] = {
    ask(searchActor, StationSearch(query, stations)).map {
      case seq: Seq[(Double, Station)] =>
        seq
      case _ =>
        List()
    }
  }
}
