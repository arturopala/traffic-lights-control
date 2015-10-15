package trafficlightscontrol.actors

import akka.actor._
import akka.pattern.ask
import scala.collection._
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.mutable.{ Set, Map }

import trafficlightscontrol.model._

object SwitchActor {
  def props(
    id:             Id,
    memberProp:     Props,
    configuration:  Configuration,
    initiallyGreen: Boolean       = false,
    skipTicks:      Int           = 0
  ): Props =
    Props(classOf[SwitchActor], id, memberProp, configuration, initiallyGreen, skipTicks)
}

/**
 * Switch is a component reacting on TickEvent and sending downstream alternately ChangeToGreenCommand or ChangeToGreenCommand.
 * @param skipTicks number of ticks to skip between command emits
 */
class SwitchActor(
    val id:            Id,
    val memberProp:    Props,
    val configuration: Configuration,
    initiallyGreen:    Boolean,
    skipTicks:         Int
) extends SingleNodeActor {

  var isGreen: Boolean = initiallyGreen
  var remainingTicksToSkip: Int = 0

  import configuration.{ timeout }

  ////////////////////////////////////////////
  // STEP 1: WAIT FOR TICK EVENT            //
  ////////////////////////////////////////////
  val receiveWhenIdle: Receive = {
    case TickEvent if (remainingTicksToSkip <= 0) =>
      remainingTicksToSkip = skipTicks
      becomeNow(receiveWhenPending)
      members ! (if (isGreen) ChangeToRedCommand else ChangeToGreenCommand)
      members ! TickEvent
      scheduleTimeout(timeout)
    case TickEvent =>
      remainingTicksToSkip = remainingTicksToSkip - 1
  }

  /////////////////////////////////////////////
  // STEP 2: WAIT FOR CHANGE ACKNOWLEDGEMENT //
  /////////////////////////////////////////////
  private val receiveWhenPending: Receive = {
    case ChangedToGreenEvent =>
      cancelTimeout()
      isGreen = true
      becomeNow(receiveWhenIdle)

    case ChangedToRedEvent =>
      cancelTimeout()
      isGreen = false
      becomeNow(receiveWhenIdle)

    case MessageIgnoredEvent(ChangeToGreenCommand | ChangeToRedCommand) =>
      cancelTimeout()
      becomeNow(receiveWhenIdle)

    case TimeoutEvent =>
      throw new TimeoutException(s"Switch ${this.id}: timeout occured when waiting for change confirmation")

  }

}
