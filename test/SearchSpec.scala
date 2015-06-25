import api.{Station, NSApi}
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.Logger
import play.api.test.{PlaySpecification, WithApplication}
import scala.concurrent.duration.DurationInt
import scala.concurrent.Await
import searching.Search._

@RunWith(classOf[JUnitRunner])
class SearchSpec extends PlaySpecification {
  "Station Search" should {

    "give no results with empty query" in new WithApplication() {
      val stations = Await.result(NSApi.stations, 10 seconds)
      stations must have length greaterThan(0)

      val results = searchStations("", stations)

      results must be empty
    }

    "bring correct station for query to top" in new WithApplication{

      val stations = Await.result(NSApi.stations, 10 seconds)
      stations must have length greaterThan(0)

      for (station <- stations) {
        searchStations(station.name, stations).head._2.name must be equalTo station.name
      }
    }

    "bring correct station for synonym query to top" in new WithApplication {

      val stations = Await.result(NSApi.stations, 10 seconds)
      stations must have length greaterThan(0)

      for (station <- stations) {
        if (station.synonyms.nonEmpty) {
          for (synoniem <- station.synonyms) {
            val station = searchStations(synoniem, stations).head._2
            station.synonyms must be contain synoniem or (synoniem must be equalTo station.name)
          }
        }
      }
    }

    "bring correct station for code query to top" in new WithApplication{

      val stations = Await.result(NSApi.stations, 10 seconds)
      stations must have length greaterThan(0)

      for (station <- stations) {
        val stationHead = searchStations(station.code, stations).head._2
        stationHead.code must be equalTo station.code
      }
    }
  }

  private def searchForIndex(query: String, comperable: (Station, String) => Boolean, placeInList: Seq[Station] => Int, stations: Seq[Station]): (Int, Int, Option[Seq[Station]]) = {
    val splitQuery = query.split(" ").flatMap(_.split("-"))
      .toSeq

    var stationName = if (splitQuery.length > 1) splitQuery.sliding(2).map(_.head).toList.mkString(" ") + " " else ""

    var i = 0
    var place = 0
    for (char <- splitQuery.last) {
      i = i+1
      stationName = stationName + char
      val stationSearch = searchStations(stationName, stations)
      if (comperable(stationSearch.head._2, query)) {
        return (i, place, Some(stationSearch.map(_._2)))
      }
      place = placeInList(stationSearch.map(_._2))
    }
    Logger.error("None found for " + query + ". Last query was " + stationName)
    (-1, -1, None)
  }

  "Station Location Search" should {
    "show the closest station for location" in new WithApplication() {
      val stations = Await.result(NSApi.stations, 10 seconds)
      stations must have length greaterThan(0)

      val stationSearch = searchStations(52.0, 4.0, stations)
      stationSearch must have length greaterThan(0)
    }

    "show the closest station for a set of locations" in new WithApplication() {
      val stations = Await.result(NSApi.stations, 10 seconds)
      stations must have length greaterThan(0)

      val sets: Seq[(Double, Double, String)] = Seq(
        (52.3770998, 4.9273675, "ASD"),
        (52.0697513, 4.3208721, "GV"),
        (52.0283907, 6.2257973, "WL"),
        (52.42, 4.2, "ZVT"),
        (52.4412957, 4.8030361, "ZD"),
        (52.516775, 6.083022, "ZL"),
        (52.090737, 6.083022, "BMN"),
        (52.090737, 5.121420, "UTM")
      )

      for (set <- sets) {
        val stationSearch = searchStations(set._1, set._2, stations)
        stationSearch.head._2.code must be equalTo set._3
      }
    }

    "show the closest station for a its own location" in new WithApplication() {
      val stations = Await.result(NSApi.stations, 10 seconds)
      stations must have length greaterThan(0)

      for (station <- stations) {
        val stationSearch = searchStations(station.coords.lat, station.coords.lon, stations)
        stationSearch.head._2.coords.lat must be equalTo station.coords.lat
        stationSearch.head._2.coords.lon must be equalTo station.coords.lon
      }
    }

    "get the designated station in less than 0.75 times the keystrokes of the length of the name, or when not, it must be in the top 5" in new WithApplication() {
      val stations = Await.result(NSApi.stations, 10 seconds).filter(_.code != "BUENDE")
      stations must have length greaterThan(0)

      for (station <- stations) {

        val searchQueries = Seq(
          Seq(
            station.names.long,
            station.names.middle,
            station.names.short
          ),
          station.synonyms
        ).flatten

        for (query <- searchQueries) {
          val (index, prevPlace, Some(foundStations)) = searchForIndex(query, {
            case (s: Station, q: String) =>
              s.equals(station)
          }, _.indexOf(station), stations)

          index must be greaterThan 0

          val length = station.name.length.asInstanceOf[Double]
          val indexDouble = index.asInstanceOf[Double]

          if (indexDouble < length * 0.75) {
            indexDouble must be lessThan (station.name.length.asInstanceOf[Double] * 0.75)
          } else {
            Logger.warn("Aware: " + station.name + " by " + query + " has been found very late (" + index + ":" + length + "), but was previously found on index " + prevPlace)
            prevPlace must be lessThan 2
          }
        }
      }
    }
  }
}
