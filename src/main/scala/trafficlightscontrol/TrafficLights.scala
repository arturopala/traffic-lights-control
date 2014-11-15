package trafficlightscontrol

import akka.actor.Actor
import scala.concurrent.duration._
import akka.actor.ActorRef
import scala.concurrent.ExecutionContext
import akka.actor.ActorLogging
import akka.actor.Stash

object ChangeToRedCommand
object ChangeToGreenCommand
object GetStatusQuery
object ChangedToRedEvent
object ChangedToGreenEvent

sealed abstract class Light(colour: String) {
  override val toString: String = s"${colour}Light"
}
object RedLight extends Light("Red")
object GreenLight extends Light("Green")
object OrangeLight extends Light("Orange")

private case class ChangeFromOrangeToRedCommand(sender: ActorRef)
private case class ChangeFromOrangeToGreenCommand(sender: ActorRef)

class TrafficLight(var status: Light = RedLight, delay: FiniteDuration = 1 seconds)(implicit executionContext: ExecutionContext)
    extends Actor with ActorLogging with Stash {

  def receive = {
    case GetStatusQuery => sender ! status
    case msg => status match {
      case RedLight => receiveWhenRed(msg)
      case GreenLight => receiveWhenGreen(msg)
      case OrangeLight => receiveWhenOrange(msg)
    }
  }

  def receiveWhenRed: Receive = {
    case ChangeToRedCommand => {
      sender ! ChangedToRedEvent
    }
    case ChangeToGreenCommand => {
      status = OrangeLight
      logStatusChange()
      context.system.scheduler.scheduleOnce(delay, self, ChangeFromOrangeToGreenCommand(sender))
    }
  }

  def receiveWhenGreen: Receive = {
    case ChangeToRedCommand => {
      status = OrangeLight
      logStatusChange()
      context.system.scheduler.scheduleOnce(delay, self, ChangeFromOrangeToRedCommand(sender))
    }
    case ChangeToGreenCommand => {
      sender ! ChangedToGreenEvent
    }
  }

  def receiveWhenOrange: Receive = {
    case ChangeFromOrangeToRedCommand(origin) => {
      status = RedLight
      logStatusChange()
      origin ! ChangedToRedEvent
      unstashAll()
    }
    case ChangeFromOrangeToGreenCommand(origin) => {
      status = GreenLight
      logStatusChange()
      origin ! ChangedToGreenEvent
      unstashAll()
    }
    case msg => stash()
  }

  def logStatusChange() = log.info(s"${self.path} : changed to $status")

}
