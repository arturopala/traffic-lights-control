package trafficlightscontrol.actors

import akka.actor._
import akka.pattern.ask
import scala.collection._
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.mutable.{ Set, Map }

import trafficlightscontrol.model._

object GroupActor {
  def props(
    id:            Id,
    memberProps:   Iterable[Props],
    configuration: Configuration
  ): Props =
    Props(classOf[GroupActor], id, memberProps, configuration)
}

/**
 * Group is a set of traffic control components (eg. lights, groups, other switches) which should be all red or green at the same time.
 */
class GroupActor(

    val id:            Id,
    val memberProps:   Iterable[Props],
    val configuration: Configuration
) extends BaseNodeActor with Stash {

  val responderSet: Set[ActorRef] = Set()
  var isGreen: Option[Boolean] = None

  import configuration.{ timeout }

  /////////////////////////////////////////
  // STATE 1: IDLE, WAITING FOR COMMANDS //
  /////////////////////////////////////////
  val receiveWhenIdle: Receive = {

    case ChangeToGreenCommand => isGreen match {
      case None | Some(false) =>
        responderSet.clear()
        context.become(receiveWhileChangingToGreen orElse receiveCommonNodeMessages)
        members ! ChangeToGreenCommand
        scheduleTimeout(timeout)
      case Some(true) =>
        recipient ! ChangedToGreenEvent
    }

    case ChangeToRedCommand => isGreen match {
      case None | Some(true) =>
        responderSet.clear()
        context.become(receiveWhileChangingToRed orElse receiveCommonNodeMessages)
        members ! ChangeToRedCommand
        scheduleTimeout(timeout)
      case Some(false) =>
        recipient ! ChangedToRedEvent
    }
  }

  ///////////////////////////////////////////////////
  // STATE 2: WAITING FOR ALL IS RED CONFIRMATION  //
  ///////////////////////////////////////////////////
  val receiveWhileChangingToRed: Receive = {

    case ChangedToRedEvent =>
      responderSet += sender()
      if (responderSet.size == members.size) {
        cancelTimeout()
        isGreen = Some(false)
        context.become(receiveWhenIdle orElse receiveCommonNodeMessages)
        recipient ! ChangedToRedEvent
      }

    case ChangeToGreenCommand => stash() // we can't avoid going red at that point

    case ChangeToRedCommand   => //ignore, already changing to red

    case TimeoutEvent =>
      throw new TimeoutException("Group ${this.id}: timeout occured when waiting for all final red acks")
  }

  ////////////////////////////////////////////////////
  // STATE 3: WAITING FOR ALL IS GREEN CONFIRMATION //
  ////////////////////////////////////////////////////
  val receiveWhileChangingToGreen: Receive = {

    case ChangedToGreenEvent =>
      responderSet += sender()
      if (responderSet.size == members.size) {
        cancelTimeout()
        isGreen = Some(true)
        context.become(receiveWhenIdle orElse receiveCommonNodeMessages)
        recipient ! ChangedToGreenEvent
      }

    case ChangeToRedCommand   => stash() // we can't avoid going green at that point

    case ChangeToGreenCommand => //ignore, already changing to red

    case TimeoutEvent =>
      throw new TimeoutException("Group ${this.id}: timeout occured when waiting for all final green acks")
  }

  override val receive = receiveWhenInitializing orElse receiveCommonNodeMessages

}
