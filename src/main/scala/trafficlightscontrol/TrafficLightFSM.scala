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
  extends Actor with ActorLogging with FSM[Light, Option[ActorRef]] with Stash {

  startWith(initialState, None)

  when(RedLight) {
    case Event(ChangeToRedCommand, _) =>
      stay replying (ChangedToRedEvent)
    case Event(ChangeToGreenCommand(_), _) =>
      goto(OrangeLight) using Some(sender)
  }

  when(GreenLight) {
    case Event(ChangeToGreenCommand(_), _) =>
      stay replying (ChangedToGreenEvent(id))
    case Event(ChangeToRedCommand, _) =>
      goto(OrangeLight) using Some(sender)
  }

  when(OrangeLight) {
    case Event(ChangeFromOrangeToGreenCommand, _) =>
      goto(GreenLight) using None
    case Event(ChangeFromOrangeToRedCommand, _) =>
      goto(RedLight) using None
    case Event(ChangeToRedCommand, _) =>
      stash()
      stay
    case Event(ChangeToGreenCommand(_), _) =>
      stash()
      stay
  }

  onTransition {
    case _ -> GreenLight =>
      stateData map (_ ! ChangedToGreenEvent(id))
    case _ -> RedLight =>
      stateData map (_ ! ChangedToRedEvent)
    case RedLight -> OrangeLight =>
      setTimer("changeToGreen", ChangeFromOrangeToGreenCommand, delay, false)
    case GreenLight -> OrangeLight =>
      setTimer("changeToRed", ChangeFromOrangeToRedCommand, delay, false)
  }

  onTransition {
    case oldState -> newState =>
      log.debug(s"transition of $id from $oldState to $newState")
      context.system.eventStream.publish(StatusEvent(id, newState))
  }

  onTransition {
    case OrangeLight -> _ =>
      unstashAll()
  }

  whenUnhandled {
    case Event(GetStatusQuery, _) => {
      sender ! StatusEvent(id, stateName)
      stay
    }
  }

  initialize()

}
