package searching

import actor.SearchActor.StationSearch
import actor.{SearchActor, NotifyActor}
import akka.actor.Props
import akka.util.Timeout
import api.Station
import play.libs.Akka
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration.DurationInt
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Search {
  implicit val timeout = Timeout(5 seconds)

  val searchActor = Akka.system.actorOf(Props[SearchActor], name = "search-actor")

  def stations(query: String, stations: Seq[Station]): Future[Seq[Station]] = {
    ask(searchActor, StationSearch(query, stations)).map {
      case seq: Seq[Station] =>
        seq
      case _ =>
        List()
    }
  }
}
