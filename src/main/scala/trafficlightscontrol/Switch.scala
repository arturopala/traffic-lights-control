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
    timeout: FiniteDuration = 10 seconds) extends Actor with ActorLogging with Stash {

  def receive = receiveWhenIdle

  // Internal switch state
  var director: Option[ActorRef] = None
  var responded: Set[ActorRef] = Set()
  var timeoutTask: Cancellable = _

  override def preStart = {
    subordinates ! SetDirectorCommand(self)
  }

  val id = 0 //FIXME

  /////////////////////////////////////////
  // STATE 1: IDLE, WAITING FOR COMMANDS //
  /////////////////////////////////////////
  def receiveWhenIdle: Receive = akka.event.LoggingReceive {

    case SetDirectorCommand(newDirector, ack) =>
      director = Option(newDirector)
      for (a <- ack; d <- director) d ! a

    case ChangeToGreenCommand =>
      responded = Set()
      context.become(receiveWhileChangingToAllRedBeforeGreen)
      subordinates ! ChangeToRedCommand
      scheduleTimeout()

    case ChangeToRedCommand =>
      responded = Set()
      context.become(receiveWhileChangingToRed)
      subordinates ! ChangeToRedCommand
      scheduleTimeout()
  }

  ///////////////////////////////////////////////////
  // STATE 2: WAITING FOR ALL IS RED CONFIRMATION  //
  ///////////////////////////////////////////////////
  def receiveWhileChangingToRed: Receive = akka.event.LoggingReceive {

    case ChangedToRedEvent =>
      responded = responded + sender()
      if (responded.size == subordinates.size) {
        timeoutTask.cancel()
        context.become(receiveWhenIdle)
        director ! ChangedToRedEvent
      }

    case ChangeToGreenCommand =>
      context.become(receiveWhileChangingToAllRedBeforeGreen)

    case ChangeToRedCommand => //ignore

    case TimeoutEvent =>
      throw new TimeoutException("timeout occured when waiting for all final red acks")
  }

  ////////////////////////////////////////////////////////
  // STATE 3: WAITING FOR ALL IS RED BEFORE GOING GREEN //
  ////////////////////////////////////////////////////////
  def receiveWhileChangingToAllRedBeforeGreen: Receive = akka.event.LoggingReceive {

    case ChangedToRedEvent =>
      responded = responded + sender()
      if (responded.size == subordinates.size) {
        timeoutTask.cancel()
        context.become(receiveWhileWaitingForGreenAck)
        subordinates(id) ! ChangeToGreenCommand
        scheduleTimeout()
      }

    case ChangeToGreenCommand => //ignore

    case ChangeToRedCommand =>
      context.become(receiveWhileChangingToRed)

    case TimeoutEvent =>
      throw new TimeoutException("timeout occured when waiting for all red acks before changing to green")
  }

  /////////////////////////////////////////////////////////
  // STATE 4: WAITING FOR CONFIRMATION FROM GREEN MEMBER //
  /////////////////////////////////////////////////////////
  def receiveWhileWaitingForGreenAck: Receive = akka.event.LoggingReceive {

    case ChangedToGreenEvent =>
      timeoutTask.cancel()
      context.become(receiveWhenIdle)
      director ! ChangedToGreenEvent
      unstashAll()

    case ChangeToGreenCommand => //ignore

    case ChangeToRedCommand   => stash()

    case TimeoutEvent =>
      throw new TimeoutException("timeout occured when waiting for final green ack")
  }

  def scheduleTimeout(): Unit = {
    timeoutTask = context.system.scheduler.scheduleOnce(timeout, self, TimeoutEvent)(context.system.dispatcher)
  }

}
