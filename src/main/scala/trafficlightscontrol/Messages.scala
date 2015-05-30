package trafficlightscontrol

import akka.actor.ActorRef

trait Message
trait Command extends Message
trait Query extends Message
trait Event extends Message

case class SetDirectorCommand(director: ActorRef, ack: Option[Event] = None) extends Command

object ChangeToRedCommand extends Command
case class ChangeToGreenCommand(id: String) extends Command
object ChangeFromOrangeCommand extends Command

object ChangedToRedEvent extends Event
object ChangedToGreenEvent extends Event

object GetStatusQuery extends Query
case class StatusEvent(id: String, state: LightState) extends Event

object GetReportQuery extends Query
case class ReportEvent(report: Map[String, LightState]) extends Event

object TimeoutEvent extends Event
object TickCommand extends Command

///////////////////
/// LIGHT STATES //
///////////////////

sealed abstract class LightState(val colour: String, val id: String) {
  override val toString: String = s"${colour}"
}

object RedLight extends LightState("Red", "R")
object GreenLight extends LightState("Green", "G")
object OrangeThenRedLight extends LightState("OrangeThenRed", "OTR")
object OrangeThenGreenLight extends LightState("OrangeThenGreen", "OTG")