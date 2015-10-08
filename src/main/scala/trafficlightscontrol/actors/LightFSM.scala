package trafficlightscontrol.actors

import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import trafficlightscontrol.model._

object LightFSM {

  def props(
    id: Id,
    initialState: LightState = RedLight,
    configuration: Configuration): Props = Props(classOf[LightFSM], id, initialState, configuration)
}

/**
 * LightFSM is a primitive building block of a traffic control system. R
 * Represents single control point with possible states: GreenLight, ChangingToRedLight, RedLight, ChangingToGreenLight.
 * @param id UUID
 * @param initalState initial state of the light
 * @param delay green <-> red switch delay
 * @param automatic should switch from yellow to red or green automatically or manually?
 */
class LightFSM(
  id: Id,
  initialState: LightState = RedLight,
  configuration: Configuration)
    extends Actor with ActorLogging with FSM[LightState, Option[ActorRef]] {

  startWith(initialState, None)

  when(RedLight) {
    case Event(ChangeToRedCommand, recipient) =>
      recipient ! ChangedToRedEvent
      stay
    case Event(ChangeToGreenCommand, _) =>
      goto(ChangingToGreenLight)
  }

  when(GreenLight) {
    case Event(ChangeToGreenCommand, recipient) =>
      recipient ! ChangedToGreenEvent
      stay
    case Event(ChangeToRedCommand, _) =>
      goto(ChangingToRedLight)
  }

  when(ChangingToRedLight) {
    case Event(CanContinueAfterDelayEvent, _) =>
      goto(RedLight)
    case Event(ChangeToGreenCommand, _) =>
      goto(ChangingToGreenLight)
  }

  when(ChangingToGreenLight) {
    case Event(CanContinueAfterDelayEvent, _) =>
      goto(GreenLight)
    case Event(ChangeToRedCommand, _) =>
      goto(ChangingToRedLight)
  }

  onTransition {
    case oldState -> newState =>
      context.system.eventStream.publish(StatusEvent(id, newState))
      log.info(s"$id: $newState")
  }

  onTransition {
    case ChangingToGreenLight -> GreenLight =>
      stateData map (_ ! ChangedToGreenEvent)
    case ChangingToRedLight -> RedLight =>
      stateData map (_ ! ChangedToRedEvent)
    case RedLight -> ChangingToGreenLight =>
      if (configuration.automatic) setTimer("changeToGreen", CanContinueAfterDelayEvent, configuration.delayRedToGreen, false)
    case GreenLight -> ChangingToRedLight =>
      if (configuration.automatic) setTimer("changeToRed", CanContinueAfterDelayEvent, configuration.delayGreenToRed, false)
  }

  whenUnhandled {
    case Event(RegisterRecipientCommand(newRecipient), _) =>
      if (stateData.isEmpty) {
        val recipient = Option(newRecipient)
        recipient ! RecipientRegisteredEvent(id)
        stay using recipient
      }
      else stay

    case Event(GetStatusQuery, _) => {
      sender ! StatusEvent(id, stateName)
      stay
    }
    case Event(_, _) => stay
  }

  initialize()

}