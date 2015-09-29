package trafficlightscontrol.actors

import akka.actor._
import akka.pattern.ask
import scala.collection._
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.mutable.{ Set, Map }

import trafficlightscontrol.model._

object GroupActor {
  def props(id: Id,
            memberProps: Iterable[Props],
            timeout: FiniteDuration = 10 seconds): Props =
    Props(classOf[GroupActor], id, memberProps, timeout)
}

/**
 * Group is a set of components (eg. lights, groups, other switches) which should be all red or green at the same time.
 */
class GroupActor(
    id: Id,
    memberProps: Iterable[Props],
    baseTimeout: FiniteDuration = 10 seconds) extends Actor with ActorLogging with Stash {

  def receive = receiveWhenInitializing orElse receiveUnhandled

  var recipient: Option[ActorRef] = None
  val members: Map[Id, ActorRef] = Map()
  var memberIds: Seq[Id] = Seq.empty

  val responderSet: Set[ActorRef] = Set()
  var timeoutTask: Cancellable = _

  var isGreen: Option[Boolean] = None

  override def preStart = {
    for (prop <- memberProps) {
      val member = context.actorOf(prop)
      member ! RegisterRecipientCommand(self)
    }
  }

  /////////////////////////////////////////////////////////////////
  // STATE 0: INITIALIZING, WAITING FOR ALL MEMBERS REGISTRATION //
  /////////////////////////////////////////////////////////////////
  val receiveWhenInitializing: Receive = {

    case RecipientRegisteredEvent(id) =>
      members.getOrElseUpdate(id, sender())
      memberIds = members.keys.toSeq
      log.debug(s"Group ${this.id}: new member registered $id")
      if (members.size == memberProps.size) {
        log.info(s"Group ${this.id} initialized. Members: ${memberIds.mkString(",")}, timeout: $baseTimeout")
        context.become(receiveWhenIdle orElse receiveUnhandled)
        unstashAll()
      }

    case ChangeToGreenCommand | ChangeToRedCommand => // ignore until initialized
      log.warning(s"Group $id not yet initialized, skipping command")
  }

  /////////////////////////////////////////
  // STATE 1: IDLE, WAITING FOR COMMANDS //
  /////////////////////////////////////////
  val receiveWhenIdle: Receive = {

    case ChangeToGreenCommand => isGreen match {
      case None | Some(false) =>
        responderSet.clear()
        context.become(receiveWhileChangingToGreen orElse receiveUnhandled)
        members ! ChangeToGreenCommand
        scheduleTimeout()
      case Some(true) =>
        recipient ! ChangedToGreenEvent
    }

    case ChangeToRedCommand => isGreen match {
      case None | Some(true) =>
        responderSet.clear()
        context.become(receiveWhileChangingToRed orElse receiveUnhandled)
        members ! ChangeToRedCommand
        scheduleTimeout()
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
        timeoutTask.cancel()
        isGreen = Some(false)
        context.become(receiveWhenIdle orElse receiveUnhandled)
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
        timeoutTask.cancel()
        isGreen = Some(true)
        context.become(receiveWhenIdle orElse receiveUnhandled)
        recipient ! ChangedToGreenEvent
      }

    case ChangeToRedCommand   => stash() // we can't avoid going green at that point

    case ChangeToGreenCommand => //ignore, already changing to red

    case TimeoutEvent =>
      throw new TimeoutException("Group ${this.id}: timeout occured when waiting for all final green acks")
  }

  val receiveUnhandled: Receive = {

    case RegisterRecipientCommand(newRecipient) =>
      if (recipient.isEmpty) {
        recipient = Option(newRecipient)
        recipient ! RecipientRegisteredEvent(id)
      }

    case other =>
      log.error(s"Group ${this.id}: command not recognized $other")
  }

  def scheduleTimeout(): Unit = {
    timeoutTask = context.system.scheduler.scheduleOnce(baseTimeout, self, TimeoutEvent)(context.system.dispatcher)
  }

}
