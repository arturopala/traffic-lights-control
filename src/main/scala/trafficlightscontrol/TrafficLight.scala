package trafficlightscontrol

import akka.actor.Actor
import scala.concurrent.duration._
import akka.actor.ActorRef
import scala.concurrent.ExecutionContext
import akka.actor.ActorLogging
import akka.actor.Stash

class TrafficLight(
  id: String,
  initialState: Light = RedLight,
  delay: FiniteDuration = 1 seconds)
    extends Actor with ActorLogging with Stash {

  var state: Light = initialState
  var origin: ActorRef = _

  def receive = {
    case GetStatusQuery => sender ! StatusEvent(id, state)
    case msg => state match {
      case RedLight    => receiveWhenRed(msg)
      case GreenLight  => receiveWhenGreen(msg)
      case OrangeLight => receiveWhenOrange(msg)
    }
  }

  def receiveWhenRed: Receive = {
    case ChangeToRedCommand => {
      sender ! ChangedToRedEvent
    }
    case ChangeToGreenCommand(id) => {
      changeStateTo(OrangeLight)
      origin = sender
      context.system.scheduler.scheduleOnce(delay, self, ChangeFromOrangeToGreenCommand)(context.system.dispatcher)
    }
  }

  def receiveWhenGreen: Receive = {
    case ChangeToRedCommand => {
      changeStateTo(OrangeLight)
      origin = sender
      context.system.scheduler.scheduleOnce(delay, self, ChangeFromOrangeToRedCommand)(context.system.dispatcher)
    }
    case ChangeToGreenCommand(id) => {
      sender ! ChangedToGreenEvent
    }
  }

  def receiveWhenOrange: Receive = {
    case ChangeFromOrangeToRedCommand => {
      changeStateTo(RedLight)
      origin ! ChangedToRedEvent
      unstashAll()
    }
    case ChangeFromOrangeToGreenCommand => {
      changeStateTo(GreenLight)
      origin ! ChangedToGreenEvent
      unstashAll()
    }
    case msg => stash()
  }

  def changeStateTo(light: Light) = {
    state = light
    context.system.eventStream.publish(StatusEvent(id, state))
  }

  changeStateTo(state)

}