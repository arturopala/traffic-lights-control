package trafficlightscontrol

import akka.actor.Actor
import scala.concurrent.duration._
import akka.actor.ActorRef
import scala.concurrent.ExecutionContext
import akka.actor.ActorLogging
import akka.actor.Stash
import akka.actor.FSM
import sun.awt.X11.XBaseWindow.InitialiseState

/**
 * LightFSM is a primitive building block of a traffic control system. R
 * Represents single control point with possible states: GreenLight, ChangingToRedLight, RedLight, ChangingToGreenLight.
 * @param id UUID
 * @param initalState initial state of the light
 * @param delay green <-> red switch delay
 * @param automatic should switch from orange to red or green automatically or manually?
 */
class LightFSM(
  id: String,
  initialState: LightState = RedLight,
  delay: FiniteDuration = 1 seconds,
  automatic: Boolean = true)
    extends Actor with ActorLogging with FSM[LightState, Option[ActorRef]] {

  startWith(initialState, None)

  when(RedLight) {
    case Event(ChangeToRedCommand, director) =>
      director ! ChangedToRedEvent
      stay
    case Event(ChangeToGreenCommand, _) =>
      goto(ChangingToGreenLight)
  }

  when(GreenLight) {
    case Event(ChangeToGreenCommand, director) =>
      director ! ChangedToGreenEvent
      stay
    case Event(ChangeToRedCommand, _) =>
      goto(ChangingToRedLight)
  }

  when(ChangingToRedLight) {
    case Event(FinalizeChange, _) =>
      goto(RedLight)
    case Event(ChangeToGreenCommand, _) =>
      goto(ChangingToGreenLight)
  }

  when(ChangingToGreenLight) {
    case Event(FinalizeChange, _) =>
      goto(GreenLight)
    case Event(ChangeToRedCommand, _) =>
      goto(ChangingToRedLight)
  }

  onTransition {
    case oldState -> newState =>
      context.system.eventStream.publish(StatusEvent(id, newState))
  }

  onTransition {
    case ChangingToGreenLight -> GreenLight =>
      stateData map (_ ! ChangedToGreenEvent)
    case ChangingToRedLight -> RedLight =>
      stateData map (_ ! ChangedToRedEvent)
    case RedLight -> ChangingToGreenLight =>
      if (automatic) setTimer("changeToGreen", FinalizeChange, delay, false)
    case GreenLight -> ChangingToRedLight =>
      if (automatic) setTimer("changeToRed", FinalizeChange, delay, false)
  }

  whenUnhandled {
    case Event(SetDirectorCommand(newDirector, ack), _) =>
      val director = Option(newDirector)
      for (a <- ack; d <- director) d ! a
      stay using director
    case Event(GetStatusQuery, _) => {
      sender ! StatusEvent(id, stateName)
      stay
    }
    case Event(_, _) => stay
  }

  initialize()

}
