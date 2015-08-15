package trafficlightscontrol

import akka.actor.ActorRef

trait Message
trait Command extends Message
trait Query extends Message
trait Event extends Message

case class RegisterDirectorCommand(director: ActorRef) extends Command
case class DirectorRegisteredEvent(id: String) extends Event

case object ChangeToRedCommand extends Command
case object ChangeToGreenCommand extends Command

case object FinalizeChange extends Command

case object ChangedToRedEvent extends Event
case object ChangedToGreenEvent extends Event

case object GetStatusQuery extends Query
case class StatusEvent(id: String, state: LightState) extends Event

case object GetReportQuery extends Query
case class ReportEvent(report: Map[String, LightState]) extends Event

case object TimeoutEvent extends Event
case object TickCommand extends Command

///////////////////
/// LIGHT STATES //
///////////////////

sealed abstract class LightState(val colour: String, val id: String) {
  override val toString: String = s"${colour}"
}

case object RedLight extends LightState("Red", "R")
case object GreenLight extends LightState("Green", "G")
case object ChangingToRedLight extends LightState("OrangeThenRed", "CTR")
case object ChangingToGreenLight extends LightState("OrangeThenGreen", "CTG")