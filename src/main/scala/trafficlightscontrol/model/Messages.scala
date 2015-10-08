package trafficlightscontrol.model

import scala.concurrent.duration._
import akka.actor.ActorRef

trait Message
trait Command extends Message
trait Query extends Message
trait Event extends Message

case class RegisterRecipientCommand(director: ActorRef) extends Command
case class RecipientRegisteredEvent(id: Id) extends Event

case object ChangeToRedCommand extends Command
case object ChangeToGreenCommand extends Command

case object CanContinueAfterDelayEvent extends Command

case object ChangedToRedEvent extends Event
case object ChangedToGreenEvent extends Event

case object GetStatusQuery extends Query
case class GetStatusQuery(id: Id) extends Query
case class StatusEvent(id: Id, state: LightState) extends Event

case object GetReportQuery extends Query
case class ReportEvent(report: Map[Id, LightState]) extends Event

case class GetPublisherQuery(p: Id => Boolean) extends Command

case object TimeoutEvent extends Event
case object TickCommand extends Command

case class InstallComponentCommand(component: Component, system: Id) extends Command
case class InstallComponentFailedEvent(component: Component, system: Id, reason: String) extends Event
case class InstallComponentSucceededEvent(component: Component, system: Id) extends Event

case class StartSystemCommand(system: Id) extends Command
case class StopSystemCommand(system: Id) extends Command
case class SystemStartFailureEvent(system: Id, reason: String) extends Event

case class SystemInfoQuery(system: Id) extends Query
case class SystemInfoEvent(system: Id, component: Component, interval: FiniteDuration, history: SystemHistory) extends Event

case object CommandIgnoredEvent extends Event