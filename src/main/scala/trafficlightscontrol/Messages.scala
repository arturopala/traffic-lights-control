package trafficlightscontrol

import akka.actor.ActorRef

trait Message
trait Command extends Message
trait Query extends Message
trait Event extends Message

object ChangeToRedCommand extends Command
case class ChangeToGreenCommand(id: String) extends Command

object ChangedToRedEvent extends Event
object ChangedToGreenEvent extends Event

object ChangeFromOrangeToRedCommand
object ChangeFromOrangeToGreenCommand

object GetStatusQuery extends Query
case class StatusEvent(id: String, state: Light) extends Event

object GetReportQuery extends Query
case class ReportEvent(report: Map[String, Light]) extends Event

object TimeoutEvent extends Event
object TickCommand extends Command

sealed abstract class Light(val colour: String, val id: String) {
  override val toString: String = s"${colour}Light"
}
object RedLight extends Light("Red", "R")
object GreenLight extends Light("Green", "G")
object OrangeLight extends Light("Orange", "O")