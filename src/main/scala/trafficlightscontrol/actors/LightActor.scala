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
 */
class LightActor(

    val id: Id,
    initialState: LightState = RedLight,
    val configuration: Configuration) extends BaseLeafActor {

  var state: LightState = initialState

  import configuration.{ delayRedToGreen, delayGreenToRed }

  def receiveByState: Receive = state match {
    case RedLight             => receiveWhenRed
    case GreenLight           => receiveWhenGreen
    case ChangingToRedLight   => receiveWhenChangingToRed
    case ChangingToGreenLight => receiveWhenChangingToGreen
  }

  val receiveQuery: Receive = {
    case GetStatusQuery => recipient ! StatusEvent(id, state)
  }

  val receiveWhenRed: Receive = {
    case ChangeToRedCommand =>
      recipient ! ChangedToRedEvent

    case ChangeToGreenCommand =>
      changeStateTo(ChangingToGreenLight)
      scheduleDelay(delayRedToGreen)
  }

  val receiveWhenGreen: Receive = {
    case ChangeToRedCommand =>
      changeStateTo(ChangingToRedLight)
      scheduleDelay(delayGreenToRed)
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
    case ChangeToRedCommand => //ignore
  }

  val receiveWhenChangingToGreen: Receive = {
    case CanContinueAfterDelayEvent =>
      changeStateTo(GreenLight)
      recipient ! ChangedToGreenEvent
    case ChangeToRedCommand =>
      changeStateTo(ChangingToRedLight)
    case ChangeToGreenCommand => //ignore
  }

  override val receive: Receive = receiveByState orElse receiveQuery orElse receiveCommonLeafMessages

  def changeStateTo(newState: LightState) = {
    state = newState
    context.system.eventStream.publish(StatusEvent(id, state))
    log.info(s"$id is $state")
    context.become(receiveByState orElse receiveQuery orElse receiveCommonLeafMessages)
  }

  changeStateTo(state)

  override val toString: String = s"Light($id,$state)"
}
