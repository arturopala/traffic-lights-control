package trafficlightscontrol

import akka.actor.Actor
import scala.concurrent.duration._
import akka.actor.ActorRef
import scala.concurrent.ExecutionContext
import akka.actor.ActorLogging
import akka.actor.Stash
import akka.actor.FSM
import sun.awt.X11.XBaseWindow.InitialiseState

class LightFSM(
  id: String,
  initialState: LightState = RedLight,
  delay: FiniteDuration = 1 seconds)
    extends Actor with ActorLogging with FSM[LightState, Option[ActorRef]] with Stash {

  startWith(initialState, None)

  when(RedLight) {
    case Event(ChangeToRedCommand, _) =>
      stay replying ChangedToRedEvent
    case Event(ChangeToGreenCommand(_), _) =>
      goto(OrangeThenGreenLight) using Some(sender)
  }

  when(GreenLight) {
    case Event(ChangeToGreenCommand(_), _) =>
      stay replying ChangedToGreenEvent
    case Event(ChangeToRedCommand, _) =>
      goto(OrangeThenRedLight) using Some(sender)
  }

  when(OrangeThenRedLight) {
    case Event(ChangeFromOrangeCommand, _) =>
      goto(RedLight) using None
    case Event(_: Command, _) =>
      stash()
      stay
  }

  when(OrangeThenGreenLight) {
    case Event(ChangeFromOrangeCommand, _) =>
      goto(GreenLight) using None
    case Event(_: Command, _) =>
      stash()
      stay
  }

  onTransition {
    case oldState -> newState =>
      //log.info(s"transition of $id from $oldState to $newState")
      context.system.eventStream.publish(StatusEvent(id, newState))
  }

  onTransition {
    case _ -> GreenLight =>
      stateData map (_ ! ChangedToGreenEvent)
    case _ -> RedLight =>
      stateData map (_ ! ChangedToRedEvent)
    case RedLight -> OrangeThenGreenLight =>
      setTimer("changeToGreen", ChangeFromOrangeCommand, delay, false)
    case GreenLight -> OrangeThenRedLight =>
      setTimer("changeToRed", ChangeFromOrangeCommand, delay, false)
  }

  onTransition {
    case OrangeThenGreenLight -> _ =>
      unstashAll()
    case OrangeThenRedLight -> _ =>
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
