import actor.NotifyActor
import actor.NotifyActor.PushNotification
import akka.actor.Props
import akka.util.Timeout
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.test.WithApplication
import play.libs.Akka
import akka.pattern.ask
import scala.concurrent.duration.DurationInt
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class PushSpec extends Specification {
  implicit val timeout = Timeout(5 seconds)

  "PushNotifications" should {
    "send notifcation" in new WithApplication {
      val notifyActor = Akka.system.actorOf(Props[NotifyActor], name = "notify-actor")

      val testNotification = ask(notifyActor, PushNotification("", "TEST", "TEST"))
      Await.result(testNotification, 2 seconds) match {
        case e: Exception =>
          throw e
        case () =>
          // YAY, push sended
          1 must equalTo(1)
      }
    }
  }
}