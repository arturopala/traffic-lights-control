package trafficlightscontrol.model

import scala.concurrent.duration._
import akka.actor.ActorRef

trait Message
trait Command extends Message
trait Query extends Message
trait Event extends Message

trait OperationCommand extends Command
trait LifecycleCommand extends Command

case class RegisterRecipientCommand(director: ActorRef) extends Command
case class RecipientRegisteredEvent(id: Id) extends Event

case object ChangeToRedCommand extends OperationCommand
case object ChangeToGreenCommand extends OperationCommand

case object CanContinueAfterDelayEvent extends Event

case object ChangedToRedEvent extends Event
case object ChangedToGreenEvent extends Event

case object GetStatusQuery extends Query
case class GetStatusQuery(id: Id) extends Query
case class StatusEvent(id: Id, state: LightState) extends Event

case object GetReportQuery extends Query
case class ReportEvent(report: Map[Id, LightState]) extends Event

case class GetPublisherQuery(p: Id => Boolean) extends Command

case class InstallComponentCommand(component: Component, system: Id) extends LifecycleCommand
case class InstallComponentFailedEvent(component: Component, system: Id, reason: String) extends Event
case class InstallComponentSucceededEvent(component: Component, system: Id) extends Event

case class StartSystemCommand(system: Id) extends LifecycleCommand
case class StopSystemCommand(system: Id) extends LifecycleCommand

case class SystemStartedEvent(system: Id) extends Event
case class SystemStartFailureEvent(system: Id, reason: String) extends Event
case class SystemStopFailureEvent(system: Id, reason: String) extends Event

case class SystemInfoQuery(system: Id) extends Query
case class SystemInfoEvent(system: Id, component: Component, interval: FiniteDuration, history: SystemHistory) extends Event

case object TickEvent extends Event
case object TimeoutEvent extends Event
case class MessageIgnoredEvent(message: Message) extends Event