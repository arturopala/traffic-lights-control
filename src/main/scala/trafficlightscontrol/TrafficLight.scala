package trafficlightscontrol

import akka.actor.Actor
import scala.concurrent.duration._
import akka.actor.ActorRef
import scala.concurrent.ExecutionContext
import akka.actor.ActorLogging
import akka.actor.Stash

trait Command
trait Query
trait Event

object GetStatusQuery extends Query
case class StatusEvent(id: String, status: Light) extends Event
object ChangeToRedCommand extends Command
case class ChangeToGreenCommand(id: String) extends Command
object ChangedToRedEvent extends Event
case class ChangedToGreenEvent(id: String) extends Event
object TimeoutEvent extends Event
object GetReportQuery extends Query
case class ReportEvent(report: Map[String, Light]) extends Event
object TickCommand extends Command

sealed abstract class Light(val colour: String, val id: String) {
  override val toString: String = s"${colour}Light"
}
object RedLight extends Light("Red", "R")
object GreenLight extends Light("Green", "G")
object OrangeLight extends Light("Orange", "O")

private case class ChangeFromOrangeToRedCommand(sender: ActorRef)
private case class ChangeFromOrangeToGreenCommand(sender: ActorRef)

class TrafficLight(
  id: String,
  var status: Light = RedLight,
  delay: FiniteDuration = 1 seconds)
    extends Actor with ActorLogging with Stash {

  def receive = {
    case GetStatusQuery => sender ! StatusEvent(id, status)
    case msg => status match {
      case RedLight => receiveWhenRed(msg)
      case GreenLight => receiveWhenGreen(msg)
      case OrangeLight => receiveWhenOrange(msg)
    }
  }

  def receiveWhenRed: Receive = {
    case ChangeToRedCommand => {
      sender ! ChangedToRedEvent
    }
    case ChangeToGreenCommand(id) => {
      changeStateTo(OrangeLight)
      context.system.scheduler.scheduleOnce(delay, self, ChangeFromOrangeToGreenCommand(sender))(context.system.dispatcher)
    }
  }

  def receiveWhenGreen: Receive = {
    case ChangeToRedCommand => {
      changeStateTo(OrangeLight)
      context.system.scheduler.scheduleOnce(delay, self, ChangeFromOrangeToRedCommand(sender))(context.system.dispatcher)
    }
    case ChangeToGreenCommand(id) => {
      sender ! ChangedToGreenEvent(id)
    }
  }

  def receiveWhenOrange: Receive = {
    case ChangeFromOrangeToRedCommand(origin) => {
      changeStateTo(RedLight)
      origin ! ChangedToRedEvent
      unstashAll()
    }
    case ChangeFromOrangeToGreenCommand(origin) => {
      changeStateTo(GreenLight)
      origin ! ChangedToGreenEvent(id)
      unstashAll()
    }
    case msg => stash()
  }

  def changeStateTo(light: Light) = {
    status = light
    context.system.eventStream.publish(StatusEvent(id, status))
  }

  changeStateTo(status)

}
