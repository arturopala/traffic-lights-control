package trafficlightscontrol

import akka.actor._
import akka.pattern.ask
import scala.collection.mutable.Set
import scala.concurrent._
import scala.concurrent.duration._

object Switch {
  sealed trait State
  object Free extends State
  object WaitingForAllRed extends State
  object WaitingForGreen extends State

  case class StateData(
    currentGreenId: Option[String] = None,
    recipientSet: Set[ActorRef] = Set.empty,
    origin: ActorRef = ActorRef.noSender)
}

import Switch._

class OnlyOneIsGreenSwitchFSM(
    val members: Map[String, ActorRef],
    timeout: FiniteDuration = 10 seconds) extends Actor with ActorLogging with FSM[State, StateData] with Stash {

  def initialState = StateData()
  val memberSet: scala.collection.Set[ActorRef] = members.values.toSet

  startWith(Free, initialState)

  when(Free) {
    case Event(ChangeToGreenCommand(id), StateData(currentGreenId, recipientSet, origin)) => {
      currentGreenId match {
        case None               => goto(WaitingForAllRed) using StateData(currentGreenId = Some(id), origin = sender())
        case Some(i) if i != id => goto(WaitingForAllRed) using StateData(currentGreenId = Some(id), origin = sender())
        case Some(i) if i == id => stay
      }
    }
    case Event(ChangeToRedCommand, _) =>
      goto(WaitingForAllRed) using StateData(origin = sender())
  }

  when(WaitingForAllRed, stateTimeout = timeout) {
    case Event(ChangedToRedEvent, state @ StateData(currentGreenId, recipientSet, origin)) =>
      memberSet.contains(sender) match {
        case false => stay
        case true => {
          recipientSet += sender
          recipientSet.size == memberSet.size match {
            case false => stay
            case true => currentGreenId match {
              case None => goto(Free) using initialState
              case _    => goto(WaitingForGreen) using state.copy(recipientSet = Set.empty)
            }
          }
        }
      }
    case Event(_: Command, _) =>
      stash()
      stay
    case Event(StateTimeout, _) =>
      stay
  }

  when(WaitingForGreen, stateTimeout = timeout) {
    case Event(ChangedToGreenEvent(id), StateData(currentGreenId, _, _)) =>
      currentGreenId.get == id match {
        case false => stay
        case true  => goto(Free) using StateData(currentGreenId = currentGreenId)
      }
    case Event(_: Command, _) =>
      stash()
      stay
    case Event(StateTimeout, _) =>
      stay
  }

  onTransition {
    case oldState -> newState =>
    //log.info(s"transition from $oldState to $newState")
  }

  onTransition {
    case Free -> WaitingForAllRed =>
      tellToMembers(ChangeToRedCommand)
    case WaitingForAllRed -> WaitingForGreen =>
      tellToMemberChangeToGreen(stateData)
    case WaitingForGreen -> Free =>
      notifyOriginAboutGreen(stateData)
    case WaitingForAllRed -> Free =>
      notifyOriginAboutRed(stateData)
  }

  onTransition {
    case _ -> Free =>
      unstashAll()
  }

  whenUnhandled {
    case Event(query: Query, _) =>
      forwardToMembers(query)
      stay
  }

  initialize()

  def tellToMemberChangeToGreen(stateData: StateData): Unit = for (i <- stateData.currentGreenId; w <- members.get(i)) { w ! ChangeToGreenCommand(i) }
  def tellToMembers(msg: AnyRef): Unit = for (w <- memberSet) { w ! msg }
  def forwardToMembers(msg: AnyRef): Unit = for (w <- memberSet) { w forward msg }
  def notifyOriginAboutGreen(stateData: StateData): Unit = stateData match { case StateData(Some(currentGreenId), _, origin) => origin ! ChangedToGreenEvent(currentGreenId) }
  def notifyOriginAboutRed(stateData: StateData): Unit = stateData match { case StateData(None, _, origin) => origin ! ChangedToRedEvent }
}
