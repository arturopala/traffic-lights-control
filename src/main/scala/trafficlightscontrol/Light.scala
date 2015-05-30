package trafficlightscontrol

import akka.actor.Actor
import scala.concurrent.duration._
import akka.actor.ActorRef
import scala.concurrent.ExecutionContext
import akka.actor.ActorLogging
import akka.actor.Stash

/**
 * Light is a primitive building block of a traffic control system.
 * Possible states: GreenLight, ChangingToRedLight, RedLight, ChangingToGreenLight.
 * @param id UUID
 * @param initalState initial state of the light
 * @param delay green <-> red switch delay
 * @param automatic should switch from orange to red or green automatically or manually?
 */
class Light(
  id: String,
  initialState: LightState = RedLight,
  delay: FiniteDuration = 1 seconds,
  automatic: Boolean = true)
    extends Actor with ActorLogging {

  var state: LightState = initialState
  var director: Option[ActorRef] = None

  def receive = /*akka.event.LoggingReceive*/ {

    case GetStatusQuery => director ! StatusEvent(id, state)
    case SetDirectorCommand(newDirector, ack) =>
      director = Option(newDirector)
      for (a <- ack; d <- director) d ! a

    case cmd: Command => state match {
      case RedLight             => receiveWhenRed(cmd)
      case GreenLight           => receiveWhenGreen(cmd)
      case ChangingToRedLight   => receiveWhenChangingToRed(cmd)
      case ChangingToGreenLight => receiveWhenChangingToGreen(cmd)
    }
  }

  def receiveWhenRed: Receive = {
    case ChangeToRedCommand =>
      director ! ChangedToRedEvent

    case ChangeToGreenCommand(id) =>
      changeStateTo(ChangingToGreenLight)
      if (automatic) context.system.scheduler.scheduleOnce(delay, self, FinalizeChange)(context.system.dispatcher)
  }

  def receiveWhenGreen: Receive = {
    case ChangeToRedCommand =>
      changeStateTo(ChangingToRedLight)
      if (automatic) context.system.scheduler.scheduleOnce(delay, self, FinalizeChange)(context.system.dispatcher)
    case ChangeToGreenCommand(id) => {
      director ! ChangedToGreenEvent
    }
  }

  def receiveWhenChangingToRed: Receive = {
    case FinalizeChange =>
      changeStateTo(RedLight)
      director ! ChangedToRedEvent
    case ChangeToGreenCommand(id) =>
      changeStateTo(ChangingToGreenLight)
    case _ => //ignore
  }

  def receiveWhenChangingToGreen: Receive = {
    case FinalizeChange =>
      changeStateTo(GreenLight)
      director ! ChangedToGreenEvent
    case ChangeToRedCommand =>
      changeStateTo(ChangingToRedLight)
    case _ => //ignore
  }

  def changeStateTo(light: LightState) = {
    state = light
    context.system.eventStream.publish(StatusEvent(id, state))
  }

  changeStateTo(state)

  override val toString: String = s"Light($id,$state)"

}
