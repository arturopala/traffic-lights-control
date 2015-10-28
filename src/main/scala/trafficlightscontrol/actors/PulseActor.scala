package trafficlightscontrol.actors

import akka.actor._
import akka.pattern.ask
import scala.collection._
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.mutable.{ Set, Map }

import trafficlightscontrol.model._

object PulseActor {
  def props(
    id:            Id,
    memberProp:    Props,
    configuration: Configuration,
    command:       Command       = ChangeToGreenCommand,
    ackEvent:      Event         = ChangedToGreenEvent,
    skipTicks:     Int           = 0
  ): Props =
    Props(classOf[PulseActor], id, memberProp, configuration, skipTicks, command, ackEvent)
}

/**
 * Pulse is a component reacting on TickEvent and sending downstream command.
 * @param skipTicks number of ticks to skip between command emits
 */
class PulseActor(
    val id:            Id,
    val memberProp:    Props,
    val configuration: Configuration,
    skipTicks:         Int,
    command:           Command,
    ackEvent:          Event
) extends SingleNodeActor {

  var remainingTicksToSkip: Int = 0

  import configuration.{ timeout }

  ////////////////////////////////////////////
  // STEP 1: WAIT FOR TICK EVENT            //
  ////////////////////////////////////////////
  val receiveWhenIdle: Receive = composeWithDefault {

    case TickEvent if (remainingTicksToSkip <= 0) =>
      remainingTicksToSkip = skipTicks
      become(receiveWhenPending)
      member ! command
      member ! TickEvent
      scheduleTimeout(timeout)

    case TickEvent =>
      remainingTicksToSkip = remainingTicksToSkip - 1
  }

  /////////////////////////////////////////////
  // STEP 2: WAIT FOR CHANGE ACKNOWLEDGEMENT //
  /////////////////////////////////////////////
  private val receiveWhenPending: Receive = composeWithDefault {

    case msg: Event if (msg == ackEvent) =>
      cancelTimeout()
      become(receiveWhenIdle)

    case MessageIgnoredEvent(cmd) if (cmd == command) =>
      cancelTimeout()
      become(receiveWhenIdle)

    case TimeoutEvent =>
      throw new TimeoutException(s"Pulse ${this.id}: timeout occured when waiting for change confirmation")

  }

}
