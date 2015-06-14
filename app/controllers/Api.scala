package controllers

import api.{Station, NSApi}
import play.api.libs.json.Json
import play.api.mvc._

import play.api.cache.Cache
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
 * Created by tomas on 10-06-15.
 */
class Api extends Controller {
  def stations = Action.async { implicit request =>
    NSApi.stations.map { stations =>
      val stationsJson = stations.map { station =>
        Json.toJson(station)
      }

      Ok(Json.obj(
        "advices" -> Json.toJson(stationsJson)
      ))
    }
  }

  def advices = Action.async {
    NSApi.advices().map { advices =>
      val advicesJson = advices.map { advices =>
        Json.toJson(advices)
      }

      Ok(Json.obj(
        "advices" -> Json.toJson(advicesJson),
        "count" -> advicesJson.size.toString
      ))
    }
  }

  def advicesFuture = Action.async {
    NSApi.advicesFuture().map { advices =>
      val advicesJson = advices.map { advices =>
        Json.toJson(advices)
      }

      Ok(Json.obj(
        "advices" -> Json.toJson(advicesJson),
        "count" -> advicesJson.size.toString
      ))
    }
  }

  def adviceFirstPossible = Action.async {
    NSApi.adviceFirstPossible().map { advices =>
      val advicesJson = advices.map { advices =>
        Json.toJson(advices)
      }

      Ok(Json.obj(
        "advices" -> Json.toJson(advicesJson),
        "count" -> advicesJson.size.toString
      ))
    }
  }

  def registerStation(user: String, from: String, to: String) = Action.async {
    Notifier.registerStation(user, from, to).map { res =>
      Ok(Json.obj("success" -> res.ok))
    }
  }

  def registerUUID(user: String, uuid: String) = Action.async {
    Notifier.registerUUID(user, uuid).map { res =>
      Ok(Json.obj("success" -> res.ok))
    }
  }
}
