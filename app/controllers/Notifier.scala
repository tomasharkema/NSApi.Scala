package controllers

import actor.NotifyActor
import actor.NotifyActor._
import akka.util.Timeout
import api.{Station, Advice, NSApi}
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

//object NotificationType extends Enumeration {
//  val PushNotification = Value("PUSH")
//  val EmailNotification = Value("EMAIL")
//}

sealed trait NotificationType {
  val value: String
}

case object EmailNotification extends NotificationType {
  val value: String = "EMAIL"
}

case object PushNotification extends NotificationType {
  val value: String = "PUSH"
}

object NotificationType {
  val values = Seq(EmailNotification, PushNotification)

  def getFromString(string: String): Option[NotificationType] = {
    values.find(_.value == string)
  }

  def getFromString(string: Option[String]): Option[NotificationType] = {
    string match {
      case Some(opt) =>
        getFromString(opt)
      case _ =>
        None
    }
  }
}

object Notifier {
  val collectionUUID = Connection.getCollection("users_uuid")
  val collectionStations = Connection.getCollection("users_stations")
  val collectionAdvices = Connection.getCollection("advices")
  val notifyActor = Akka.system.actorOf(Props[NotifyActor], name = "notify-actor")

  implicit val timeout = Timeout(5 seconds)

  def registerUUID(user: String, registerType: NotificationType,  uuid: String) = {
    val newDoc = BSONDocument(
      "name" -> user,
      "type" -> registerType.value,
      "uuid" -> uuid
    )

    val updateDoc = BSONDocument(
      "name" -> user,
      "type" -> registerType.value,
      "uuid" -> uuid,
      "updated" -> BSONDateTime(DateTime.now().getMillis)
    )

    collectionUUID.update(newDoc, updateDoc, upsert = true)
  }

  def registerStation(user: String, from: Station, to: Station) = {
    val newDoc = BSONDocument(
      "name" -> user
    )

    val updateDoc = BSONDocument(
      "name" -> user,
      "from" -> from.code,
      "to" -> to.code,
      "updated" -> BSONDateTime(DateTime.now().getMillis)
    )

    collectionStations.update(newDoc, updateDoc, upsert = true)
  }

  private def getStationsAndUsers = {
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

      (NotificationType.getFromString(notTypeString), uuidString) match {
        case (Some(notType), Some(uuid)) =>
          Some(notType, uuid)
        case (_, _) =>
          None
      }
    }.filter(_.isDefined).flatten)
  }

  private def notifyUser(advice: Advice, user: String) = {
    Logger.debug("Notify users: " + user + " " + advice.vertrekVertraging)

    getNotificationTypesForUser(user).map(_.map {
      case (_, email) =>
        SendEmailNotification(email, "Notification", advice.statusString)
      case (_, pushToken) =>
        SendPushNotification(pushToken, "Notification", advice.statusString)
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
    getStationsAndUsers.map { sauFuture =>
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