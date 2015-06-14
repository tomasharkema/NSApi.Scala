package controllers

import api.{Advice, NSApi}
import connection.Connection
import org.joda.time.DateTime
import play.api.libs.iteratee.Iteratee
import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.bson.BSONDocument

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by tomas on 14-06-15.
 */

sealed abstract class Updateable
case object New extends Updateable
case object NeedsUpdate extends Updateable
case object NoUpdateNeeded extends Updateable

object Notifier {
  val collectionUUID = Connection.getCollection("users_uuid")
  val collectionStations = Connection.getCollection("users_stations")
  val collectionAdvices = Connection.getCollection("advices")

  def registerUUID(user: String, uuid: String) = {
    val newDoc = BSONDocument(
      "name" -> user,
      "uuid" -> uuid
    )

    val updateDoc = BSONDocument(
      "name" -> user,
      "uuid" -> uuid,
      "updated" -> BSONDateTime(DateTime.now().getMillis)
    )

    collectionUUID.update(newDoc, updateDoc, upsert = true)
  }

  def registerStation(user: String, from: String, to: String) = {
    val newDoc = BSONDocument(
      "name" -> user
    )

    val updateDoc = BSONDocument(
      "name" -> user,
      "from" -> from,
      "to" -> to
    )

    collectionStations.update(newDoc, updateDoc, upsert = true)
  }

  private def getStationsAndUsers() = {
    val cursor = collectionStations.find(BSONDocument()).cursor[BSONDocument]
    val list = cursor.collect[List]()

    list.map { future =>
      future.groupBy(t =>
        (t.getAs[String]("from").get, t.getAs[String]("to").get)
      ).mapValues(e =>
        e.map(e =>
          e.getAs[String]("name").get).toSet
        )
    }
  }

  private def notifyUser(advice: Advice, user: String) = {
   println("Notify users: " + user, advice.vertrekVertraging)
  }

  private def updateIfNeeded(advice: Advice): Future[Updateable] = {
    val find = BSONDocument("advice" -> advice.request.toString)
    val cursor = collectionAdvices.find(find).cursor[BSONDocument]
    cursor.headOption.map {
      case Some(obj) =>
        if (obj.getAs[String]("vertraging").getOrElse("") == advice.vertrekVertraging.getOrElse("")) {
          NoUpdateNeeded
        } else {
          collectionAdvices.update(find, BSONDocument("advice" -> advice.request.toString, "vertraging" -> advice.vertrekVertraging.getOrElse("")))
          NeedsUpdate
        }

      case None =>
        collectionAdvices.save(BSONDocument("advice" -> advice.request.toString, "vertraging" -> advice.vertrekVertraging.getOrElse("")))
        New
    }
  }

  def notifyUsers() = {
    getStationsAndUsers().map { sauFuture =>
      sauFuture.foreach { sau =>
        val stations = sau._1
        val users = sau._2

        NSApi.adviceFirstPossible(stations._1, stations._2).map(_.foreach { advice =>
          updateIfNeeded(advice).map {
            case NeedsUpdate => users.foreach(notifyUser(advice, _))
            case _ => println("No update needed for " + advice.request)
          }
        })
      }
    }
  }
}