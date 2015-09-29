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
            timeout: FiniteDuration = 10 seconds,
            strategy: SwitchStrategy = SwitchStrategy.RoundRobin): Props =
    Props(classOf[SwitchActor], id, memberProps, timeout, strategy)
}

/**
 * Switch is a set of components (eg. lights, groups, other switches) amongst which only one may be green at once.
 */
class SwitchActor(
    id: Id,
    memberProps: Iterable[Props],
    baseTimeout: FiniteDuration = 10 seconds,
    strategy: SwitchStrategy = SwitchStrategy.RoundRobin) extends Actor with ActorLogging with Stash {

  def receive = receiveWhenInitializing orElse receiveUnhandled

  var recipient: Option[ActorRef] = None
  val members: Map[Id, ActorRef] = Map()
  var memberIds: Seq[Id] = Seq.empty

  val responderSet: Set[ActorRef] = Set()
  var timeoutTask: Cancellable = _

  var isGreen = false
  var greenMemberId: Id = ""
  var nextGreenId: Id = _

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
      log.debug(s"Switch ${this.id}: new member registered $id")
      if (members.size == memberProps.size) {
        log.info(s"Switch ${this.id} initialized. Members: ${memberIds.mkString(",")}, timeout: $baseTimeout")
        context.become(receiveWhenIdle orElse receiveUnhandled)
        unstashAll()
      }

    case ChangeToGreenCommand | ChangeToRedCommand => // ignore until initialized
      log.warning(s"Switch $id not yet initialized, skipping command")
  }

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
        context.become(receiveWhileChangingToAllRedBeforeGreen orElse receiveUnhandled)
        members ! ChangeToRedCommand
        scheduleTimeout()
      }
      else {
        throw new IllegalStateException(s"Switch ${this.id}: Member $nextGreenId not found")
      }

    case ChangeToRedCommand =>
      responderSet.clear()
      context.become(receiveWhileChangingToRed orElse receiveUnhandled)
      members ! ChangeToRedCommand
      scheduleTimeout()
  }

  ///////////////////////////////////////////////////
  // STATE 2: WAITING FOR ALL IS RED CONFIRMATION  //
  ///////////////////////////////////////////////////
  val receiveWhileChangingToRed: Receive = {

    case ChangedToRedEvent =>
      responderSet += sender()
      if (responderSet.size == members.size) {
        timeoutTask.cancel()
        isGreen = false
        context.become(receiveWhenIdle orElse receiveUnhandled)
        recipient ! ChangedToRedEvent
      }

    case ChangeToGreenCommand =>
      context.become(receiveWhileChangingToAllRedBeforeGreen orElse receiveUnhandled) // enable going green in the next step

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
        timeoutTask.cancel()
        context.become(receiveWhileWaitingForGreenAck orElse receiveUnhandled)
        members.get(nextGreenId) match {
          case Some(member) =>
            member ! ChangeToGreenCommand
            scheduleTimeout()
          case None =>
            throw new IllegalStateException(s"Switch ${this.id}: Member $nextGreenId not found")
        }
      }

    case ChangeToGreenCommand => //ignore

    case ChangeToRedCommand =>
      context.become(receiveWhileChangingToRed orElse receiveUnhandled) // avoid going green in the next step

    case TimeoutEvent =>
      throw new TimeoutException("Switch ${this.id}: timeout occured when waiting for all red acks before changing to green")
  }

  /////////////////////////////////////////////////////////
  // STATE 4: WAITING FOR CONFIRMATION FROM GREEN MEMBER //
  /////////////////////////////////////////////////////////
  val receiveWhileWaitingForGreenAck: Receive = {

    case ChangedToGreenEvent =>
      timeoutTask.cancel()
      isGreen = true
      greenMemberId = nextGreenId
      context.become(receiveWhenIdle orElse receiveUnhandled)
      recipient ! ChangedToGreenEvent
      unstashAll()

    case ChangeToGreenCommand => //ignore, already changing to green

    case ChangeToRedCommand   => stash() // we can't avoid going green at that point

    case TimeoutEvent =>
      throw new TimeoutException("Switch ${this.id}: timeout occured when waiting for final green ack")
  }

  val receiveUnhandled: Receive = {

    case RegisterRecipientCommand(newRecipient) =>
      if (recipient.isEmpty) {
        recipient = Option(newRecipient)
        recipient ! RecipientRegisteredEvent(id)
      }

    case other =>
      log.error(s"Switch ${this.id}: command not recognized $other")
  }

  def scheduleTimeout(): Unit = {
    timeoutTask = context.system.scheduler.scheduleOnce(baseTimeout, self, TimeoutEvent)(context.system.dispatcher)
  }

}
