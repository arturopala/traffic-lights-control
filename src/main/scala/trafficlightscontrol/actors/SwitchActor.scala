package trafficlightscontrol.actors

import akka.actor._
import akka.pattern.ask
import scala.collection._
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.mutable.{ Set, Map }

import trafficlightscontrol.model._

object SwitchActor {
  def props(id: Id,
            memberProps: Iterable[Props],
            configuration: Configuration,
            strategy: SwitchStrategy = SwitchStrategy.RoundRobin): Props =
    Props(classOf[SwitchActor], id, memberProps, configuration, strategy)
}

/**
 * Switch is a set of components (eg. lights, groups, other switches) amongst which only one may be green at once.
 */
class SwitchActor(

    val id: Id,
    val memberProps: Iterable[Props],
    val configuration: Configuration,
    strategy: SwitchStrategy = SwitchStrategy.RoundRobin) extends BaseNodeActor with Stash {

  val responderSet: Set[ActorRef] = Set()

  var isGreen = false
  var greenMemberId: Id = ""
  var nextGreenId: Id = _

  import configuration.{ timeout, switchDelay }

  /////////////////////////////////////////
  // STATE 1: IDLE, WAITING FOR COMMANDS //
  /////////////////////////////////////////
  val receiveWhenIdle: Receive = {

    case ChangeToGreenCommand =>
      nextGreenId = strategy(greenMemberId, memberIds)
      if (isGreen && nextGreenId == greenMemberId) {
        recipient ! ChangedToGreenEvent
      }
      else if (members.contains(nextGreenId)) {
        responderSet.clear()
        context.become(receiveWhileChangingToAllRedBeforeGreen orElse receiveCommonNodeMessages)
        members ! ChangeToRedCommand
        scheduleTimeout(timeout)
      }
      else {
        throw new IllegalStateException(s"Switch ${this.id}: Member $nextGreenId not found")
      }

    case ChangeToRedCommand =>
      responderSet.clear()
      context.become(receiveWhileChangingToRed orElse receiveCommonNodeMessages)
      members ! ChangeToRedCommand
      scheduleTimeout(timeout)
  }

  ///////////////////////////////////////////////////
  // STATE 2: WAITING FOR ALL IS RED CONFIRMATION  //
  ///////////////////////////////////////////////////
  val receiveWhileChangingToRed: Receive = {

    case ChangedToRedEvent =>
      responderSet += sender()
      if (responderSet.size == members.size) {
        cancelTimeout()
        isGreen = false
        context.become(receiveWhenIdle orElse receiveCommonNodeMessages)
        recipient ! ChangedToRedEvent
      }

    case ChangeToGreenCommand =>
      context.become(receiveWhileChangingToAllRedBeforeGreen orElse receiveCommonNodeMessages) // enable going green in the next step

    case ChangeToRedCommand => //ignore, already changing to red

    case TimeoutEvent =>
      throw new TimeoutException("Switch ${this.id}: timeout occured when waiting for all final red acks")
  }

  ////////////////////////////////////////////////////////
  // STATE 3: WAITING FOR ALL IS RED BEFORE GOING GREEN //
  ////////////////////////////////////////////////////////
  val receiveWhileChangingToAllRedBeforeGreen: Receive = {

    case ChangedToRedEvent =>
      responderSet += sender()
      if (responderSet.size == members.size) {
        cancelTimeout()
        scheduleDelay(switchDelay)
        isGreen = false
        context.become(receiveWhileDelayedBeforeGreen orElse receiveCommonNodeMessages)
      }

    case ChangeToGreenCommand => //ignore

    case ChangeToRedCommand =>
      context.become(receiveWhileChangingToRed orElse receiveCommonNodeMessages) // avoid going green in the next step

    case TimeoutEvent =>
      throw new TimeoutException("Switch ${this.id}: timeout occured when waiting for all red acks before changing to green")
  }

  //////////////////////////////////////////////////////////
  // STATE 4: WAITING WHEN DELAY BEFORE GOING GREEN       //
  //////////////////////////////////////////////////////////
  val receiveWhileDelayedBeforeGreen: Receive = {

    case CanContinueAfterDelayEvent =>
      members.get(nextGreenId) match {
        case Some(member) =>
          member ! ChangeToGreenCommand
          scheduleTimeout(timeout)
          context.become(receiveWhileWaitingForGreenAck orElse receiveCommonNodeMessages)
        case None =>
          throw new IllegalStateException(s"Switch ${this.id}: Member $nextGreenId not found")
      }

    case ChangeToGreenCommand => //ignore

    case ChangedToRedEvent =>
      cancelDelay()
      isGreen = false
      context.become(receiveWhenIdle orElse receiveCommonNodeMessages)
      recipient ! ChangedToRedEvent
  }

  /////////////////////////////////////////////////////////
  // STATE 5: WAITING FOR CONFIRMATION FROM GREEN MEMBER //
  /////////////////////////////////////////////////////////
  val receiveWhileWaitingForGreenAck: Receive = {

    case ChangedToGreenEvent =>
      cancelTimeout()
      isGreen = true
      greenMemberId = nextGreenId
      context.become(receiveWhenIdle orElse receiveCommonNodeMessages)
      recipient ! ChangedToGreenEvent
      unstashAll()

    case ChangeToGreenCommand => //ignore, already changing to green

    case ChangeToRedCommand   => stash() // we can't avoid going green at that point

    case TimeoutEvent =>
      throw new TimeoutException("Switch ${this.id}: timeout occured when waiting for final green ack")
  }

  override val receive = receiveWhenInitializing orElse receiveCommonNodeMessages

}
