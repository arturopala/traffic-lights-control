package trafficlightscontrol

import akka.actor._
import akka.pattern.ask
import scala.collection._
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.mutable.{ Set, Map }

object Group {
  def props(id: String,
            memberProps: Seq[Props],
            timeout: FiniteDuration = 10 seconds): Props =
    Props(classOf[Group], id, memberProps, timeout)
}

/**
 * Group is a set of components (eg. lights, groups, other switches) which should be all red or green at the same time.
 */
class Group(
    id: String,
    memberProps: Seq[Props],
    baseTimeout: FiniteDuration = 10 seconds) extends Actor with ActorLogging with Stash {

  def receive = receiveWhenInitializing orElse receiveUnhandled

  var recipient: Option[ActorRef] = None
  val members: Map[String, ActorRef] = Map()
  var memberIds: Seq[String] = Seq.empty

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
      log.debug(s"Switch ${this.id}: new member registered $id")
      if (members.size == memberProps.size) {
        log.info(s"Switch ${this.id} initialized. Members: ${memberIds.mkString(",")}")
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
      throw new TimeoutException("Switch ${this.id}: timeout occured when waiting for all final red acks")
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
      throw new TimeoutException("Switch ${this.id}: timeout occured when waiting for all final green acks")
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
