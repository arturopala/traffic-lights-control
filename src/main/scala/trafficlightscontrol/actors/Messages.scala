package trafficlightscontrol.actors

import trafficlightscontrol.model._
import akka.actor.ActorRef

trait Message
trait Command extends Message
trait Query extends Message
trait Event extends Message

case class RegisterRecipientCommand(director: ActorRef) extends Command
case class RecipientRegisteredEvent(id: Id) extends Event

case object ChangeToRedCommand extends Command
case object ChangeToGreenCommand extends Command

case object FinalizeChange extends Command

case object ChangedToRedEvent extends Event
case object ChangedToGreenEvent extends Event

case object GetStatusQuery extends Query
case class GetStatusQuery(id: Id) extends Query
case class StatusEvent(id: Id, state: LightState) extends Event

case object GetReportQuery extends Query
case class ReportEvent(report: Map[Id, LightState]) extends Event

case object TimeoutEvent extends Event
case object TickCommand extends Command