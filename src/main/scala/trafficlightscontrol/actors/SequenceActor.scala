package trafficlightscontrol.actors

import akka.actor._
import akka.pattern.ask
import scala.collection._
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.mutable.{ Set, Map }

import trafficlightscontrol.model._

object SequenceActor {
  def props(
    id:            Id,
    memberProps:   Iterable[Props],
    configuration: Configuration,
    strategy:      SequenceStrategy = SequenceStrategy.RoundRobin
  ): Props =
    Props(classOf[SequenceActor], id, memberProps, configuration, strategy)
}

/**
 * Sequence is a set of components (eg. lights, groups, other sequencees) amongst which only one may be green at once.
 */
class SequenceActor(

    val id:            Id,
    val memberProps:   Iterable[Props],
    val configuration: Configuration,
    strategy:          SequenceStrategy = SequenceStrategy.RoundRobin
) extends BaseNodeActor with Stash {

  val responderSet: Set[ActorRef] = Set()

  var isGreen = false
  var greenMemberId: Id = ""
  var nextGreenId: Id = _

  import configuration.{ timeout, sequenceDelay }

  /////////////////////////////////////////
  // STATE 1: IDLE, WAITING FOR COMMANDS //
  /////////////////////////////////////////
  val receiveWhenIdle: Receive = composeWithDefault {

    case ChangeToGreenCommand =>
      nextGreenId = strategy(greenMemberId, memberIds)
      if (isGreen && nextGreenId == greenMemberId) {
        recipient ! ChangedToGreenEvent
      }
      else if (members.contains(nextGreenId)) {
        responderSet.clear()
        becomeNow(receiveWhileChangingToAllRedBeforeGreen)
        members ! ChangeToRedCommand
        scheduleTimeout(timeout)
      }
      else {
        throw new IllegalStateException(s"Sequence ${this.id}: Member $nextGreenId not found")
      }

    case ChangeToRedCommand =>
      responderSet.clear()
      becomeNow(receiveWhileChangingToRed)
      members ! ChangeToRedCommand
      scheduleTimeout(timeout)
  }

  ///////////////////////////////////////////////////
  // STATE 2: WAITING FOR ALL IS RED CONFIRMATION  //
  ///////////////////////////////////////////////////
  private val receiveWhileChangingToRed: Receive = composeWithDefault {

    case ChangedToRedEvent | MessageIgnoredEvent(ChangeToRedCommand) =>
      responderSet += sender()
      if (responderSet.size == members.size) {
        cancelTimeout()
        isGreen = false
        becomeNow(receiveWhenIdle)
        recipient ! ChangedToRedEvent
      }

    case ChangeToGreenCommand =>
      becomeNow(receiveWhileChangingToAllRedBeforeGreen) // enable going green in the next step

    case ChangeToRedCommand => //ignore, already changing to red

    case TimeoutEvent =>
      throw new TimeoutException(s"Sequence ${this.id}: timeout occured when waiting for all final red acks")
  }

  ////////////////////////////////////////////////////////
  // STATE 3: WAITING FOR ALL IS RED BEFORE GOING GREEN //
  ////////////////////////////////////////////////////////
  private val receiveWhileChangingToAllRedBeforeGreen: Receive = composeWithDefault {

    case ChangedToRedEvent | MessageIgnoredEvent(ChangeToRedCommand) =>
      responderSet += sender()
      if (responderSet.size == members.size) {
        cancelTimeout()
        scheduleDelay(sequenceDelay)
        isGreen = false
        becomeNow(receiveWhileDelayedBeforeGreen)
      }

    case ChangeToGreenCommand => //ignore

    case ChangeToRedCommand =>
      becomeNow(receiveWhileChangingToRed) // avoid going green in the next step

    case TimeoutEvent =>
      throw new TimeoutException(s"Sequence ${this.id}: timeout occured when waiting for all red acks before changing to green")
  }

  //////////////////////////////////////////////////////////
  // STATE 4: WAITING WHEN DELAY BEFORE GOING GREEN       //
  //////////////////////////////////////////////////////////
  private val receiveWhileDelayedBeforeGreen: Receive = composeWithDefault {

    case CanContinueAfterDelayEvent =>
      members.get(nextGreenId) match {
        case Some(member) =>
          member ! ChangeToGreenCommand
          scheduleTimeout(timeout)
          becomeNow(receiveWhileWaitingForGreenAck)
        case None =>
          throw new IllegalStateException(s"Sequence ${this.id}: Member $nextGreenId not found")
      }

    case ChangeToGreenCommand => //ignore

    case ChangedToRedEvent =>
      cancelDelay()
      isGreen = false
      becomeNow(receiveWhenIdle)
      recipient ! ChangedToRedEvent
  }

  /////////////////////////////////////////////////////////
  // STATE 5: WAITING FOR CONFIRMATION FROM GREEN MEMBER //
  /////////////////////////////////////////////////////////
  private val receiveWhileWaitingForGreenAck: Receive = composeWithDefault {

    case ChangedToGreenEvent | MessageIgnoredEvent(ChangeToGreenCommand) =>
      cancelTimeout()
      isGreen = true
      greenMemberId = nextGreenId
      becomeNow(receiveWhenIdle)
      recipient ! ChangedToGreenEvent
      unstashAll()

    case ChangeToGreenCommand => //ignore, already changing to green

    case ChangeToRedCommand   => stash() // we can't avoid going green at that point

    case TimeoutEvent =>
      throw new TimeoutException(s"Sequence ${this.id}: timeout occured when waiting for final green ack")
  }

}
