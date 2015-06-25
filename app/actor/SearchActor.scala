package actor

import actor.SearchActor.{StationLocationSearch, StationSearch}
import akka.actor.{Props, Actor}
import akka.actor.Actor.Receive
import api.Station
import searching.Search.searchStations

/**
 * Created by tomas on 20-06-15.
 */

object SearchActor {
  def props = Props[NotifyActor]

  case class StationSearch(query: String, stations: Seq[Station])
  case class StationLocationSearch(lat: Double, lon: Double, stations: Seq[Station])
}

class SearchActor extends Actor {
  override def receive: Receive = {
    case StationSearch(query, stations) =>
      sender() ! searchStations(query, stations)
    case StationLocationSearch(lat, lon, stations) =>
      sender() ! searchStations(lat, lon, stations)
  }
}
