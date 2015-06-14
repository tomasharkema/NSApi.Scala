package actor

import akka.actor.Actor
import controllers.Notifier
import play.api.Logger

/**
 * Created by tomas on 14-06-15.
 */
class PushActor extends Actor {

  def receive = {
    case _ => {
      Logger.debug("Looking for events to notify...")
      Notifier.notifyUsers()
    }
  }
}