package trafficlightscontrol

import akka.actor._
import akka.pattern.ask
import scala.collection._
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.mutable.{ Set, Map }

/**
 * Switch is a set of components (eg. lights, groups, other switches) amongst which only one may be green at once.
 */
class Switch(
    val memberProps: Seq[Props],
    baseTimeout: FiniteDuration = 10 seconds) extends Actor with ActorLogging with Stash {

  def receive = receiveWhenIdle

  var director: Option[ActorRef] = None
  val members: Map[String, ActorRef] = Map()

  val responded: Set[ActorRef] = Set()
  var timeoutTask: Cancellable = _

  override def preStart = {
    for (prop <- memberProps) {
      context.actorOf(prop) ! RegisterDirectorCommand(self)
    }
  }

  var currentGreenId: String = ""
  var nextGreenId: String = _

  /////////////////////////////////////////
  // STATE 1: IDLE, WAITING FOR COMMANDS //
  /////////////////////////////////////////
  def receiveWhenIdle: Receive = akka.event.LoggingReceive {

    case RegisterDirectorCommand(newDirector) =>
      if (director.isEmpty) {
        director = Option(newDirector)
        director ! DirectorRegisteredEvent("")
      }

    case DirectorRegisteredEvent(id) =>
      members.getOrElseUpdate(id, sender())

    case ChangeToGreenCommand =>
      nextGreenId = "1" //FIXME
      if (nextGreenId != currentGreenId && members.contains(nextGreenId)) {
        responded.clear()
        context.become(receiveWhileChangingToAllRedBeforeGreen)
        members ! ChangeToRedCommand
        scheduleTimeout()
      }

    case ChangeToRedCommand =>
      responded.clear()
      context.become(receiveWhileChangingToRed)
      members ! ChangeToRedCommand
      scheduleTimeout(members.size * 2)
  }

  ///////////////////////////////////////////////////
  // STATE 2: WAITING FOR ALL IS RED CONFIRMATION  //
  ///////////////////////////////////////////////////
  def receiveWhileChangingToRed: Receive = akka.event.LoggingReceive {

    case ChangedToRedEvent =>
      responded += sender()
      if (responded.size == members.size) {
        timeoutTask.cancel()
        context.become(receiveWhenIdle)
        director ! ChangedToRedEvent
      }

    case ChangeToGreenCommand =>
      context.become(receiveWhileChangingToAllRedBeforeGreen) // enable going green in the next step

    case ChangeToRedCommand => //ignore, already changing to red

    case TimeoutEvent =>
      throw new TimeoutException("baseTimeout occured when waiting for all final red acks")
  }

  ////////////////////////////////////////////////////////
  // STATE 3: WAITING FOR ALL IS RED BEFORE GOING GREEN //
  ////////////////////////////////////////////////////////
  def receiveWhileChangingToAllRedBeforeGreen: Receive = akka.event.LoggingReceive {

    case ChangedToRedEvent =>
      responded += sender()
      if (responded.size == members.size) {
        timeoutTask.cancel()
        context.become(receiveWhileWaitingForGreenAck)
        members(nextGreenId) ! ChangeToGreenCommand
        scheduleTimeout()
      }

    case ChangeToGreenCommand => //ignore

    case ChangeToRedCommand =>
      context.become(receiveWhileChangingToRed) // avoid going green in the next step

    case TimeoutEvent =>
      throw new TimeoutException("baseTimeout occured when waiting for all red acks before changing to green")
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

    case ChangeToGreenCommand => //ignore, already changing to green

    case ChangeToRedCommand   => stash() // we can't avoid green at that point

    case TimeoutEvent =>
      throw new TimeoutException("baseTimeout occured when waiting for final green ack")
  }

  def scheduleTimeout(factor: Int = 1): Unit = {
    timeoutTask = context.system.scheduler.scheduleOnce(baseTimeout * factor, self, TimeoutEvent)(context.system.dispatcher)
  }

}
