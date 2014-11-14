package trafficlightscontrol

import akka.actor.Actor
import TrafficLights._
import scala.concurrent.duration._
import akka.actor.ActorRef
import TrafficLights._
import scala.concurrent.ExecutionContext
import akka.actor.ActorLogging

object TrafficLights {

  object ChangeToRedCommand
  object ChangeToGreenCommand
  object GetStatusQuery
  object ChangedToRedEvent
  object ChangedToGreenEvent

  sealed abstract class Light(colour: String) {
    override val toString: String = s"${colour}Light"
  }
  object RedLight extends Light("Red")
  object GreenLight extends Light("Green")
  object OrangeLight extends Light("Orange")

}

class TrafficLight(var status: Light = RedLight, period: FiniteDuration = 1 seconds)(implicit executionContext: ExecutionContext) extends Actor with ActorLogging {

  case class ChangeFromOrangeToRedCommand(sender: ActorRef)

  def receive = {
    case ChangeToRedCommand => {
      status = OrangeLight
      logStatus()
      context.system.scheduler.scheduleOnce(period, self, ChangeFromOrangeToRedCommand(sender))
    }
    case ChangeFromOrangeToRedCommand(origin) => {
      status = RedLight
      logStatus()
      origin ! ChangedToRedEvent
    }
    case ChangeToGreenCommand => {
      status = GreenLight
      logStatus()
      sender ! ChangedToGreenEvent
    }
    case GetStatusQuery => {
      sender ! status
    }
  }

  def logStatus() = log.info(s"${self.path} : lights changed to $status")

}
