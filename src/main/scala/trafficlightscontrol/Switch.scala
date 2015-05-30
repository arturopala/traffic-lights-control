package trafficlightscontrol

import akka.actor._
import akka.pattern.ask
import scala.collection._
import scala.concurrent._
import scala.concurrent.duration._

/**
 * Switch is a set of subordinates (eg. lights, groups, other switches) amongst which only one may be green at once.
 */
class Switch(
    val subordinates: Seq[ActorRef],
    timeout: FiniteDuration = 10 seconds) extends Actor with ActorLogging {

  def receive = receiveWhenIdle

  // Internal switch state
  var director: Option[ActorRef] = None
  var responded: Set[ActorRef] = Set()
  var timeoutTask: Cancellable = _
  var nextGreen: Option[ActorRef] = None

  override def preStart = {
    subordinates ! SetDirectorCommand(self)
  }

  val id = 0 //FIXME

  def receiveWhenIdle: Receive = akka.event.LoggingReceive {

    case SetDirectorCommand(newDirector, ack) =>
      director = Option(newDirector)
      for (a <- ack; d <- director) d ! a

    case ChangeToGreenCommand =>
      nextGreen = Option(subordinates(id))
      responded = Set()
      context.become(receiveWhileChangingToAllRedBeforeGreen)
      subordinates foreach (_ ! ChangeToRedCommand)
      scheduleTimeout()

    case ChangeToRedCommand =>
      responded = Set()
      context.become(receiveWhileChangingToRed)
      subordinates ! ChangeToRedCommand
      scheduleTimeout()
  }

  def receiveWhileChangingToRed: Receive = akka.event.LoggingReceive {
    case ChangedToRedEvent =>
      responded = responded + sender()
      if (responded.size == subordinates.size) {
        timeoutTask.cancel()
        nextGreen = None
        context.become(receiveWhenIdle)
        director ! ChangedToRedEvent
      }

    case TimeoutEvent =>
      throw new TimeoutException("timeout occured when waiting for all final red acks")
  }

  def receiveWhileChangingToAllRedBeforeGreen: Receive = akka.event.LoggingReceive {
    case ChangedToRedEvent =>
      responded = responded + sender()
      if (responded.size == subordinates.size) {
        timeoutTask.cancel()
        context.become(receiveWhileWaitingForGreenAck)
        nextGreen ! ChangeToGreenCommand
        scheduleTimeout()
      }

    case TimeoutEvent =>
      throw new TimeoutException("timeout occured when waiting for all red acks before changing to green")
  }

  def receiveWhileWaitingForGreenAck: Receive = akka.event.LoggingReceive {
    case ChangedToGreenEvent =>
      timeoutTask.cancel()
      nextGreen = None
      context.become(receiveWhenIdle)
      director ! ChangedToGreenEvent

    case TimeoutEvent =>
      throw new TimeoutException("timeout occured when waiting for final green ack")
  }

  def scheduleTimeout(): Unit = {
    timeoutTask = context.system.scheduler.scheduleOnce(timeout, self, TimeoutEvent)(context.system.dispatcher)
  }

}
