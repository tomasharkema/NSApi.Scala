package actor

import java.util.Calendar

import akka.actor.Actor
import controllers.Notifier
import play.api.Logger

/**
 * Created by tomas on 14-06-15.
 */
class PushActor extends Actor {
  def receive = {
    case _ =>
      Logger.debug("Looking for events to notify...")
      val now = Calendar.getInstance()
      val currentHour = now.get(Calendar.HOUR_OF_DAY)

      if (currentHour > 6 && currentHour < 20) {
        Notifier.notifyUsers()
      }
  }
}