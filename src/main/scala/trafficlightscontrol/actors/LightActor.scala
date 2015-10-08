package trafficlightscontrol.actors

import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import trafficlightscontrol.model._

object LightActor {

  def props(
    id: Id,
    initialState: LightState = RedLight,
    configuration: Configuration): Props = Props(classOf[LightActor], id, initialState, configuration)
}

/**
 * Light is a primitive building block of a traffic control system.
 * Possible states: GreenLight, ChangingToRedLight, RedLight, ChangingToGreenLight.
 * @param id UUID
 * @param initalState initial state of the light
 * @param delay green <-> red switch delay
 * @param automatic should switch from yellow to red or green automatically or manually?
 */
class LightActor(
  id: Id,
  initialState: LightState = RedLight,
  configuration: Configuration)
    extends Actor with ActorLogging {

  var state: LightState = initialState
  var recipient: Option[ActorRef] = None

  val receive: Receive = {

    case GetStatusQuery => recipient ! StatusEvent(id, state)

    case RegisterRecipientCommand(newRecipient) =>
      if (recipient.isEmpty) {
        recipient = Option(newRecipient)
        recipient ! RecipientRegisteredEvent(id)
      }

    case cmd: Command => state match {
      case RedLight             => receiveWhenRed(cmd)
      case GreenLight           => receiveWhenGreen(cmd)
      case ChangingToRedLight   => receiveWhenChangingToRed(cmd)
      case ChangingToGreenLight => receiveWhenChangingToGreen(cmd)
    }
  }

  val receiveWhenRed: Receive = {
    case ChangeToRedCommand =>
      recipient ! ChangedToRedEvent

    case ChangeToGreenCommand =>
      changeStateTo(ChangingToGreenLight)
      if (configuration.automatic) context.system.scheduler.scheduleOnce(configuration.delayRedToGreen, self, CanContinueAfterDelayEvent)(context.system.dispatcher)
  }

  val receiveWhenGreen: Receive = {
    case ChangeToRedCommand =>
      changeStateTo(ChangingToRedLight)
      if (configuration.automatic) context.system.scheduler.scheduleOnce(configuration.delayGreenToRed, self, CanContinueAfterDelayEvent)(context.system.dispatcher)
    case ChangeToGreenCommand => {
      recipient ! ChangedToGreenEvent
    }
  }

  val receiveWhenChangingToRed: Receive = {
    case CanContinueAfterDelayEvent =>
      changeStateTo(RedLight)
      recipient ! ChangedToRedEvent
    case ChangeToGreenCommand =>
      changeStateTo(ChangingToGreenLight)
    case _ => //ignore
  }

  val receiveWhenChangingToGreen: Receive = {
    case CanContinueAfterDelayEvent =>
      changeStateTo(GreenLight)
      recipient ! ChangedToGreenEvent
    case ChangeToRedCommand =>
      changeStateTo(ChangingToRedLight)
    case _ => //ignore
  }

  def changeStateTo(light: LightState) = {
    state = light
    context.system.eventStream.publish(StatusEvent(id, state))
    log.info(s"$id: $state")
  }

  changeStateTo(state)

  override val toString: String = s"Light($id,$state)"

}
