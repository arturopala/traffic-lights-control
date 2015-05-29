package trafficlightscontrol

import akka.actor.Actor
import scala.concurrent.duration._
import akka.actor.ActorRef
import scala.concurrent.ExecutionContext
import akka.actor.ActorLogging
import akka.actor.Stash

/**
 * Light is a primitive building block of a traffic control system.
 * Possible states: GreenLight, OrangeThenRedLight, RedLight, OrangeThenGreenLight.
 */
class Light(
  id: String,
  initialState: LightState = RedLight,
  delay: FiniteDuration = 1 seconds)
    extends Actor with ActorLogging with Stash {

  var state: LightState = initialState
  var origin: ActorRef = _

  def receive = {
    case GetStatusQuery => sender ! StatusEvent(id, state)
    case msg => state match {
      case RedLight             => receiveWhenRed(msg)
      case GreenLight           => receiveWhenGreen(msg)
      case OrangeThenRedLight   => receiveWhenOrangeBeforeRed(msg)
      case OrangeThenGreenLight => receiveWhenOrangeBeforeGreen(msg)
    }
  }

  def receiveWhenRed: Receive = {
    case ChangeToRedCommand =>
      sender ! ChangedToRedEvent

    case ChangeToGreenCommand(id) =>
      changeStateTo(OrangeThenGreenLight)
      origin = sender()
      context.system.scheduler.scheduleOnce(delay, self, ChangeFromOrangeCommand)(context.system.dispatcher)
  }

  def receiveWhenGreen: Receive = {
    case ChangeToRedCommand =>
      changeStateTo(OrangeThenRedLight)
      origin = sender()
      context.system.scheduler.scheduleOnce(delay, self, ChangeFromOrangeCommand)(context.system.dispatcher)
    case ChangeToGreenCommand(id) => {
      sender ! ChangedToGreenEvent
    }
  }

  def receiveWhenOrangeBeforeRed: Receive = {
    case ChangeFromOrangeCommand =>
      changeStateTo(RedLight)
      origin ! ChangedToRedEvent
      unstashAll()
    case msg => stash()
  }

  def receiveWhenOrangeBeforeGreen: Receive = {
    case ChangeFromOrangeCommand =>
      changeStateTo(GreenLight)
      origin ! ChangedToGreenEvent
      unstashAll()
    case ChangeToRedCommand =>
      changeStateTo(OrangeThenRedLight)
    case msg => stash()
  }

  def changeStateTo(light: LightState) = {
    state = light
    context.system.eventStream.publish(StatusEvent(id, state))
  }

  changeStateTo(state)

}
