package trafficlightscontrol.actors

import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import trafficlightscontrol.model._

object LightActor {

  def props(
    id:            Id,
    initialState:  LightState    = RedLight,
    configuration: Configuration
  ): Props = Props(classOf[LightActor], id, initialState, configuration)
}

/**
 * LightActor is a primitive building block of a traffic control system.
 * Possible states: GreenLight, ChangingToRedLight, RedLight, ChangingToGreenLight.
 */
class LightActor(
    val id:            Id,
    initialState:      LightState    = RedLight,
    val configuration: Configuration
) extends BaseLeafActor {

  var state: LightState = initialState

  import configuration.{ delayRedToGreen, delayGreenToRed }

  def receiveByState(): Receive = state match {
    case RedLight | UnknownLight => receiveWhenRed
    case GreenLight              => receiveWhenGreen
    case ChangingToRedLight      => receiveWhenChangingToRed
    case ChangingToGreenLight    => receiveWhenChangingToGreen
  }

  private val receiveQuery: Receive = {
    case GetStatusQuery => signal(StateChangedEvent(id, state))
  }

  /////////////////////////////////////
  // STATE 1: IDLE WHEN STATE IS RED //
  /////////////////////////////////////
  private val receiveWhenRed: Receive = composeWithDefault {

    case ChangeToRedCommand =>
      signal(ChangedToRedEvent)

    case ChangeToGreenCommand =>
      changeStateTo(ChangingToGreenLight)
      scheduleDelay(delayRedToGreen)

  }

  ///////////////////////////////////////
  // STATE 2: IDLE WHEN STATE IS GREEN //
  ///////////////////////////////////////
  private val receiveWhenGreen: Receive = composeWithDefault {

    case ChangeToRedCommand =>
      changeStateTo(ChangingToRedLight)
      scheduleDelay(delayGreenToRed)

    case ChangeToGreenCommand => {
      signal(ChangedToGreenEvent)
    }

  }

  ///////////////////////////////////////////////
  // STATE 3: PENDING CHANGE FROM GREEN TO RED //
  ///////////////////////////////////////////////
  private val receiveWhenChangingToRed: Receive = composeWithDefault {

    case CanContinueAfterDelayEvent =>
      changeStateTo(RedLight)
      signal(ChangedToRedEvent)

    case ChangeToGreenCommand =>
      changeStateTo(ChangingToGreenLight)

    case ChangeToRedCommand => //ignore

  }

  ///////////////////////////////////////////////
  // STATE 4: PENDING CHANGE FROM RED TO GREEN //
  ///////////////////////////////////////////////
  private val receiveWhenChangingToGreen: Receive = composeWithDefault {

    case CanContinueAfterDelayEvent =>
      changeStateTo(GreenLight)
      signal(ChangedToGreenEvent)

    case ChangeToRedCommand =>
      changeStateTo(ChangingToRedLight)

    case ChangeToGreenCommand => //ignore

  }

  def changeStateTo(newState: LightState) = {
    state = newState
    publish(state)
    become(receiveByState())
  }

  changeStateTo(state)

  override def composeWithDefault(receive: Receive): Receive = super.composeWithDefault(receive orElse receiveQuery)

  override val receive: Receive = receiveByState()
  override val toString: String = s"Light($id,$state)"
}
