package controllers

import javax.inject.Inject

import api.{Station, NSApi}
import global.Global
import play.api.libs.json.Json
import play.api.mvc._

import play.api.cache._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import searching.Search

import scala.concurrent.Future

/**
 * Created by tomas on 10-06-15.
 */
class Api @Inject() (cached: Cached) extends Controller {

  def stations = cached("stations") {
    Action {
      val stationsJson = Global.stationsCache.map { station =>
        Json.toJson(station)
      }

      Ok(Json.obj(
        "stations" -> Json.toJson(stationsJson)
      ))
    }
  }

  def advices(from: String, to: String) = Action.async {
    NSApi.advices(from, to).map { advices =>
      val advicesJson = advices.map { advices =>
        Json.toJson(advices)
      }

      Ok(Json.obj(
        "advices" -> Json.toJson(advicesJson),
        "count" -> advicesJson.size
      ))
    }
  }

  def advicesFuture(from: String, to: String) = Action.async {
    NSApi.advicesFuture(from, to).map { advices =>
      val advicesJson = advices.map { advices =>
        Json.toJson(advices)
      }

      Ok(Json.obj(
        "advices" -> Json.toJson(advicesJson),
        "count" -> advicesJson.size
      ))
    }
  }

  def adviceFirstPossible(from: String, to: String) = Action.async {
    NSApi.adviceFirstPossible(from, to).map { advices =>
      val advicesJson = advices.map { advices =>
        Json.toJson(advices)
      }

      Ok(Json.obj(
        "advice" -> Json.toJson(advicesJson)
      ))
    }
  }

  def registerStation(user: String, fromString: String, toString: String) = Action.async {
    // from to validation

    val fromStation = Global.stationsCache.find(_.code == fromString)
    val toStation = Global.stationsCache.find(_.code == toString)

    (fromStation, toStation) match {
      case (Some(from), Some(to)) =>
        Notifier.registerStation(user, from, to).map { res =>
          Ok(Json.obj("success" -> res.ok))
        }
      case _ =>
        Future.apply(NotFound(Json.obj("success" -> false)))
    }
  }

  def registerUUID(user: String, registerType: String, uuid: String) = Action.async {
    // registerType validation

    NotificationType.getFromString(registerType) match {
      case Some(regType) =>
        Notifier.registerUUID(user, regType, uuid).map { res =>
          Ok(Json.obj("success" -> res.ok))
        }
      case _ =>
        Future.apply(NotFound(Json.obj("success" -> false)))
    }
  }

  def search(query: String) = cached("search." + query) {
    Action.async {
      for {
        stations <- Search.stations(query, Global.stationsCache)
      } yield
        Ok(
          Json.obj(
            "q" -> query,
            "stations" -> Json.toJson(stations.map {
              case (index, station) =>
                Json.obj("index" -> index, "station" -> station)
            })
          )
        )
    }
  }

  def searchNearest(lat: Double, lon: Double) = cached("search." + lat + ":" + lon) {
    Action.async {
      for {
        stations <- Search.stations(lat, lon, Global.stationsCache)
      } yield
        Ok(Json.obj(
          "lat" -> lat,
          "lon" -> lon,
          "stations" -> Json.toJson(stations.map {
            case (index, station) =>
              Json.obj("distance" -> index, "station" -> station)
          })
        ))
    }
  }
}
