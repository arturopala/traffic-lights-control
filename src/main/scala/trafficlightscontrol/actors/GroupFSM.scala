package trafficlightscontrol.actors

import akka.actor._
import akka.pattern.ask
import scala.collection.mutable.{ Set, Map }
import scala.concurrent._
import scala.concurrent.duration._

import trafficlightscontrol.model._

object GroupFSM {
  sealed trait State
  object Initializing extends State
  object Idle extends State
  object WaitingForAllRed extends State
  object WaitingForAllGreen extends State

  case class StateData(
    responderSet: Set[ActorRef]   = Set.empty,
    isGreen:      Option[Boolean] = None
  )

  def props(
    id:            String,
    memberProps:   Iterable[Props],
    configuration: Configuration
  ): Props =
    Props(classOf[GroupFSM], id, memberProps, configuration)
}

import GroupFSM._

/**
 * GroupFSM is a set of components (eg. lights, groups, other sequencees) which should be all red or green at the same time.
 */
class GroupFSM(
    id:            String,
    memberProps:   Iterable[Props],
    configuration: Configuration
) extends Actor with ActorLogging with LoggingFSM[State, StateData] with Stash {

  var recipient: Option[ActorRef] = None
  val members: Map[String, ActorRef] = Map()
  var memberIds: Seq[String] = Seq.empty

  val timeout = configuration.timeout

  startWith(Initializing, StateData())

  when(Initializing) {
    case Event(RecipientRegisteredEvent(id), _) =>
      members.getOrElseUpdate(id, sender())
      memberIds = members.keys.toSeq
      log.debug(s"Group ${this.id}: new member registered $id")
      if (members.size == memberProps.size) {
        log.info(s"Group ${this.id} initialized. Members: ${memberIds.mkString(",")}, timeout: $timeout")
        goto(Idle)
      }
      else stay
    case Event(ChangeToGreenCommand | ChangeToRedCommand, _) =>
      log.warning(s"Group $id not yet initialized, skipping command")
      stay
  }

  when(Idle) {
    case Event(ChangeToGreenCommand, StateData(_, isGreen)) => isGreen match {
      case None | Some(false) =>
        goto(WaitingForAllGreen)
      case Some(true) =>
        recipient ! ChangedToGreenEvent
        stay
    }
    case Event(ChangeToRedCommand, StateData(_, isGreen)) => isGreen match {
      case None | Some(true) =>
        goto(WaitingForAllRed)
      case Some(false) =>
        recipient ! ChangedToRedEvent
        stay
    }
  }

  when(WaitingForAllRed, stateTimeout = timeout) {
    case Event(ChangedToRedEvent, state @ StateData(responderSet, _)) =>
      responderSet += sender
      responderSet.size == members.size match {
        case false => stay
        case true  => goto(Idle) using state.copy(responderSet = Set.empty, isGreen = Some(false))
      }
    case Event(ChangeToGreenCommand, _) =>
      stash()
      stay
    case Event(ChangeToRedCommand, _) =>
      stay
    case Event(StateTimeout, _) =>
      throw new TimeoutException("timeout occured when waiting for all red acks")
  }

  when(WaitingForAllGreen, stateTimeout = timeout) {
    case Event(ChangedToGreenEvent, state @ StateData(responderSet, _)) =>
      responderSet += sender
      responderSet.size == members.size match {
        case false => stay
        case true  => goto(Idle) using state.copy(responderSet = Set.empty, isGreen = Some(true))
      }
    case Event(ChangeToGreenCommand, _) =>
      stay
    case Event(ChangeToRedCommand, _) =>
      stash()
      stay
    case Event(StateTimeout, _) =>
      throw new TimeoutException("timeout occured when waiting for all green acks")
  }

  onTransition {
    case Idle -> WaitingForAllRed =>
      members.values.foreach(_ ! ChangeToRedCommand)
    case Idle -> WaitingForAllGreen =>
      members.values.foreach(_ ! ChangeToGreenCommand)
    case WaitingForAllGreen -> Idle =>
      recipient ! ChangedToGreenEvent
    case WaitingForAllRed -> Idle =>
      recipient ! ChangedToRedEvent
  }

  onTransition {
    case _ -> Idle =>
      unstashAll()
  }

  whenUnhandled {
    case Event(RegisterRecipientCommand(newRecipient), _) =>
      if (recipient.isEmpty) {
        recipient = Option(newRecipient)
        recipient ! RecipientRegisteredEvent(id)
      }
      stay
    case Event(other, state) =>
      log.error(s"Unhandled $other from $sender when $stateName using $state")
      stay
  }

  initialize()

  for (prop <- memberProps) {
    val member = context.actorOf(prop)
    member ! RegisterRecipientCommand(self)
  }

}
