import api.NSApi
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.test.WithApplication
import scala.concurrent.duration.DurationInt
import scala.concurrent.Await
import searching.Search.searchStations

@RunWith(classOf[JUnitRunner])
class SearchSpec extends Specification {
  "Station Search" should {
    "bring correct station for query to top" in new WithApplication{

      val stations = Await.result(NSApi.stations, 10 seconds)
      stations must have length greaterThan(0)

      for (station <- stations) {
        searchStations(station.name, stations).head._2.name must be equalTo(station.name)
      }
    }

    "bring correct station for synoniem query to top" in new WithApplication{

      val stations = Await.result(NSApi.stations, 10 seconds)
      stations must have length greaterThan(0)

      for (station <- stations) {
        if (station.synoniemen.nonEmpty) {
          for (synoniem <- station.synoniemen) {
            val station = searchStations(synoniem, stations).head._2
            station.synoniemen must be contain synoniem or (station.name must be equalTo(synoniem))
          }
        }
      }
    }

    "bring correct station for code query to top" in new WithApplication{

      val stations = Await.result(NSApi.stations, 10 seconds)
      stations must have length greaterThan(0)

      for (station <- stations) {
        val stationHead = searchStations(station.code, stations).head._2
        stationHead.code must be equalTo(station.code)
      }
    }
  }
}
