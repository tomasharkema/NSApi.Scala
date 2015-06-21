import api.NSApi
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.test.WithApplication
import scala.concurrent.duration.DurationInt
import scala.concurrent.Await
import searching.Search._

@RunWith(classOf[JUnitRunner])
class SearchSpec extends Specification {
  "Station Search" should {
    "bring correct station for query to top" in new WithApplication{

      val stations = Await.result(NSApi.stations, 10 seconds)
      stations must have length greaterThan(0)

      for (station <- stations) {
        searchStations(station.name, stations).head._2.name must be equalTo station.name
      }
    }

    "bring correct station for synoniem query to top" in new WithApplication{

      val stations = Await.result(NSApi.stations, 10 seconds)
      stations must have length greaterThan(0)

      for (station <- stations) {
        if (station.synonyms.nonEmpty) {
          for (synoniem <- station.synonyms) {
            val station = searchStations(synoniem, stations).head._2
            station.synonyms must be contain synoniem or (station.name must be equalTo synoniem)
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
        (52.4412957, 4.8030361, "ZD")
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
  }
}
