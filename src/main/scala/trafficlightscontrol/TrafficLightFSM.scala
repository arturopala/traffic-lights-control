package trafficlightscontrol

import akka.actor.Actor
import scala.concurrent.duration._
import akka.actor.ActorRef
import scala.concurrent.ExecutionContext
import akka.actor.ActorLogging
import akka.actor.Stash
import akka.actor.FSM
import sun.awt.X11.XBaseWindow.InitialiseState

class TrafficLightFSM(
  id: String,
  initialState: Light = RedLight,
  delay: FiniteDuration = 1 seconds)
    extends Actor with ActorLogging with FSM[Light, ActorRef] with Stash {

  startWith(initialState, ActorRef.noSender)

  when(RedLight) {
    case Event(ChangeToRedCommand, _) =>
      stay
    case Event(ChangeToGreenCommand(_), _) =>
      goto(OrangeLight) using sender
  }

  when(GreenLight) {
    case Event(ChangeToGreenCommand(i), _) =>
      stay
    case Event(ChangeToRedCommand, _) =>
      goto(OrangeLight) using sender
  }

  when(OrangeLight) {
    case Event(ChangeFromOrangeToGreenCommand, _) =>
      goto(GreenLight) using ActorRef.noSender
    case Event(ChangeFromOrangeToRedCommand, _) =>
      goto(RedLight) using ActorRef.noSender
    case Event(ChangeToRedCommand, _) =>
      stash()
      stay
    case Event(ChangeToGreenCommand(_), _) =>
      stash()
      stay
  }

  onTransition {
    case OrangeLight -> GreenLight =>
      stateData ! ChangedToGreenEvent(id)
    case OrangeLight -> RedLight =>
      stateData ! ChangedToRedEvent
    case RedLight -> OrangeLight =>
      setTimer("changeToGreen", ChangeFromOrangeToGreenCommand, delay, false)
    case GreenLight -> OrangeLight =>
      setTimer("changeToRed", ChangeFromOrangeToRedCommand, delay, false)
  }

  onTransition {
    case OrangeLight -> _ =>
      unstashAll()
  }

  onTransition {
    case (_, newState) =>
      context.system.eventStream.publish(StatusEvent(id, newState))
  }

  whenUnhandled {
    case Event(GetStatusQuery, _) => {
      sender ! StatusEvent(id, stateName)
      stay
    }
  }

  initialize()

}
