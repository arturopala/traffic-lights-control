package trafficlightscontrol.actors

import akka.actor._
import akka.pattern.ask
import scala.collection.mutable.{ Set, Map }
import scala.concurrent._
import scala.concurrent.duration._

import trafficlightscontrol.model._

object SwitchFSM {
  sealed trait State
  object Initializing extends State
  object Idle extends State
  object WaitingForAllRed extends State
  object WaitingForAllRedBeforeGreen extends State
  object WaitingForGreen extends State

  case class StateData(
    greenMemberId: Id,
    responderSet: Set[ActorRef] = Set.empty,
    isGreen: Boolean = false)

  def props(id: Id,
            memberProps: Iterable[Props],
            timeout: FiniteDuration = 10 seconds,
            strategy: SwitchStrategy = SwitchStrategy.RoundRobin): Props =
    Props(classOf[SwitchFSM], id, memberProps, timeout, strategy)
}

import SwitchFSM._

/**
 * SwitchFSM is a set of components (eg. lights, groups, other switches) amongst which only one may be green at once.
 */
class SwitchFSM(
    id: Id,
    memberProps: Iterable[Props],
    timeout: FiniteDuration,
    strategy: SwitchStrategy) extends Actor with ActorLogging with LoggingFSM[State, StateData] with Stash {

  var recipient: Option[ActorRef] = None
  val members: Map[Id, ActorRef] = Map()
  var memberIds: Seq[Id] = Seq.empty

  startWith(Initializing, StateData(""))

  when(Initializing) {
    case Event(RecipientRegisteredEvent(id), _) =>
      members.getOrElseUpdate(id, sender())
      memberIds = members.keys.toSeq
      log.debug(s"Switch ${this.id}: new member registered $id")
      if (members.size == memberProps.size) {
        log.info(s"Switch ${this.id} initialized. Members: ${memberIds.mkString(",")}, timeout: $timeout")
        goto(Idle)
      }
      else stay
    case Event(ChangeToGreenCommand | ChangeToRedCommand, _) =>
      log.warning(s"Switch $id not yet initialized, skipping command")
      stay
  }

  when(Idle) {
    case Event(ChangeToGreenCommand, StateData(greenMemberId, _, isGreen)) => {
      val nextGreenId = strategy(greenMemberId, memberIds)
      if (isGreen && nextGreenId == greenMemberId) {
        recipient ! ChangedToGreenEvent
        stay
      }
      else if (members.contains(nextGreenId)) {
        goto(WaitingForAllRedBeforeGreen) using StateData(greenMemberId = nextGreenId, isGreen = isGreen)
      }
      else {
        throw new IllegalStateException(s"Switch ${this.id}: Member $nextGreenId not found")
      }
    }
    case Event(ChangeToRedCommand, _) =>
      goto(WaitingForAllRed)
  }

  when(WaitingForAllRed, stateTimeout = timeout) {
    case Event(ChangedToRedEvent, state @ StateData(_, responderSet, _)) =>
      responderSet += sender
      responderSet.size == members.size match {
        case false => stay
        case true  => goto(Idle) using state.copy(responderSet = Set.empty, isGreen = false)
      }
    case Event(ChangeToGreenCommand, _) =>
      goto(WaitingForAllRedBeforeGreen)
    case Event(ChangeToRedCommand, _) =>
      stay
    case Event(StateTimeout, _) =>
      throw new TimeoutException("timeout occured when waiting for all final red acks")
  }

  when(WaitingForAllRedBeforeGreen, stateTimeout = timeout) {
    case Event(ChangedToRedEvent, state @ StateData(_, responderSet, _)) =>
      responderSet += sender
      (responderSet.size == members.size) match {
        case false =>
          stay
        case true =>
          goto(WaitingForGreen) using state.copy(responderSet = Set.empty, isGreen = false)
      }
    case Event(ChangeToGreenCommand, stateData) =>
      stay
    case Event(ChangeToRedCommand, _) =>
      goto(WaitingForAllRed)
    case Event(StateTimeout, _) =>
      throw new TimeoutException("timeout occured when waiting for all red acks before changing to green")
  }

  when(WaitingForGreen, stateTimeout = timeout) {
    case Event(ChangedToGreenEvent, state) =>
      goto(Idle) using state.copy(isGreen = true)
    case Event(ChangeToGreenCommand, _) =>
      stay
    case Event(ChangeToRedCommand, _) =>
      stash()
      stay
    case Event(StateTimeout, _) =>
      throw new TimeoutException("timeout occured when waiting for final green ack")
  }

  onTransition {
    case Idle -> WaitingForAllRed =>
      members.values.foreach(_ ! ChangeToRedCommand)
    case Idle -> WaitingForAllRedBeforeGreen =>
      members.values.foreach(_ ! ChangeToRedCommand)
    case WaitingForAllRedBeforeGreen -> WaitingForGreen =>
      members(stateData.greenMemberId) ! ChangeToGreenCommand
    case WaitingForGreen -> Idle =>
      recipient ! ChangedToGreenEvent
    case WaitingForAllRed -> Idle =>
      recipient ! ChangedToRedEvent
  }

  onTransition {
    case WaitingForGreen -> Idle =>
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
