package controllers

import actor.NotifyActor
import actor.NotifyActor._
import akka.util.Timeout
import api.{Advice, NSApi}
import connection.Connection
import org.joda.time.DateTime
import play.api.Logger
import play.libs.Akka
import reactivemongo.bson._
import reactivemongo.bson.BSONDocument

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import akka.actor._
import akka.pattern.{ ask, pipe }

import scala.concurrent.duration.DurationInt
/**
 * Created by tomas on 14-06-15.
 */

sealed abstract class Updateable
case object New extends Updateable
case object NeedsUpdate extends Updateable
case object NoUpdateNeeded extends Updateable

object NotificationType extends Enumeration {
  val PushNotification = Value("PUSH")
  val EmailNotification = Value("EMAIL")
}

object Notifier {
  val collectionUUID = Connection.getCollection("users_uuid")
  val collectionStations = Connection.getCollection("users_stations")
  val collectionAdvices = Connection.getCollection("advices")
  val notifyActor = Akka.system.actorOf(Props[NotifyActor], name = "notify-actor")

  implicit val timeout = Timeout(5 seconds)

  def registerUUID(user: String, registerType: String,  uuid: String) = {
    val newDoc = BSONDocument(
      "name" -> user,
      "type" -> registerType,
      "uuid" -> uuid
    )

    val updateDoc = BSONDocument(
      "name" -> user,
      "type" -> registerType,
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
      "to" -> to,
      "updated" -> BSONDateTime(DateTime.now().getMillis)
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

  private def getNotificationTypesForUser(user: String) = {
    val userCollection = collectionUUID.find(BSONDocument("name" -> user)).cursor[BSONDocument].collect[List]()
    userCollection.map(_.map { doc =>
      val notTypeString = doc.getAs[String]("type")
      val uuidString = doc.getAs[String]("uuid")

      (notTypeString, uuidString) match {
        case (notType, uuid) =>
          Some(NotificationType.withName(notType.get), uuid.get)
        case (_, _) =>
          None
      }
    }.filter(_.isDefined).flatten)
  }

  private def notifyUser(advice: Advice, user: String) = {
    Logger.debug("Notify users: " + user + " " + advice.vertrekVertraging)

    getNotificationTypesForUser(user).map(_.map {
      case (NotificationType.EmailNotification, email) =>
        EmailNotification(email, "Notification", advice.statusString)
      case (NotificationType.PushNotification, pushToken) =>
        PushNotification(pushToken, "Notification", advice.statusString)
    }).map(ask(notifyActor, _))
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

  // make this more modular without side-effects
  def notifyUsers() = {
    getStationsAndUsers().map { sauFuture =>
      sauFuture.foreach { sau =>
        val stations = sau._1
        val users = sau._2

        NSApi.adviceFirstPossible(stations._1, stations._2).map(_.foreach { advice =>
          updateIfNeeded(advice).map {
            case NeedsUpdate =>
              Logger.debug("Notify " + users.size + " users about " + advice.request + " " + advice.vertrekVertraging)
              users.foreach(notifyUser(advice, _))
            case _ =>
              Logger.debug("No update needed for " + advice.request + " " + advice.vertrekVertraging)
//              users.foreach(notifyUser(advice, _))
          }
        })
      }
    }
  }
}