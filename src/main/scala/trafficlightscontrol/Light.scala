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
    extends Actor with ActorLogging {

  var state: LightState = initialState
  var director: Option[ActorRef] = None

  def receive = akka.event.LoggingReceive {

    case GetStatusQuery => director ! StatusEvent(id, state)
    case SetDirectorCommand(newDirector, ack) =>
      director = Option(newDirector)
      for (a <- ack; d <- director) d ! a

    case cmd: Command => state match {
      case RedLight             => receiveWhenRed(cmd)
      case GreenLight           => receiveWhenGreen(cmd)
      case OrangeThenRedLight   => receiveWhenOrangeBeforeRed(cmd)
      case OrangeThenGreenLight => receiveWhenOrangeBeforeGreen(cmd)
    }
  }

  def receiveWhenRed: Receive = {
    case ChangeToRedCommand =>
      director ! ChangedToRedEvent

    case ChangeToGreenCommand(id) =>
      changeStateTo(OrangeThenGreenLight)
      context.system.scheduler.scheduleOnce(delay, self, ChangeFromOrangeCommand)(context.system.dispatcher)
  }

  def receiveWhenGreen: Receive = {
    case ChangeToRedCommand =>
      changeStateTo(OrangeThenRedLight)
      context.system.scheduler.scheduleOnce(delay, self, ChangeFromOrangeCommand)(context.system.dispatcher)
    case ChangeToGreenCommand(id) => {
      director ! ChangedToGreenEvent
    }
  }

  def receiveWhenOrangeBeforeRed: Receive = {
    case ChangeFromOrangeCommand =>
      changeStateTo(RedLight)
      director ! ChangedToRedEvent
    case ChangeToGreenCommand(id) =>
      changeStateTo(OrangeThenGreenLight)
    case _ => //ignore
  }

  def receiveWhenOrangeBeforeGreen: Receive = {
    case ChangeFromOrangeCommand =>
      changeStateTo(GreenLight)
      director ! ChangedToGreenEvent
    case ChangeToRedCommand =>
      changeStateTo(OrangeThenRedLight)
    case _ => //ignore
  }

  def changeStateTo(light: LightState) = {
    state = light
    context.system.eventStream.publish(StatusEvent(id, state))
  }

  changeStateTo(state)

  override val toString: String = s"Light($id,$state)"

}
