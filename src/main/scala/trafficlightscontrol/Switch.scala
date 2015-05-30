package trafficlightscontrol

import akka.actor._
import akka.pattern.ask
import scala.collection._
import scala.concurrent._
import scala.concurrent.duration._

/**
 * Switch is a set of other components (eg. lights) amongst which only one may be green at once.
 */
class Switch(val workers: Map[String, ActorRef], timeout: FiniteDuration = 10 seconds) extends Actor with ActorLogging with Stash {

  def receive = receiveWhenFree

  var responses: Set[ActorRef] = Set()
  var currentGreenId: String = ""

  override def preStart = {
    for (w <- workers.values) w ! SetDirectorCommand(self)
  }

  def receiveWhenFree: Receive = {
    case ChangeToGreenCommand(id) => {
      if (id != currentGreenId) {
        workers.get(id) foreach { target: ActorRef =>
          {
            responses = Set()
            workers.values foreach (_ ! ChangeToRedCommand)
            val timeoutTask = context.system.scheduler.scheduleOnce(timeout, self, TimeoutEvent)(context.system.dispatcher)
            val orginalSender = sender
            context.become(receiveRedEvents(sender, timeoutTask) {
              target ! ChangeToGreenCommand(id)
              val nextTimeoutTask = context.system.scheduler.scheduleOnce(timeout, self, TimeoutEvent)(context.system.dispatcher)
              context.become(receiveFinalGreenEventWhenBusy(id, orginalSender, nextTimeoutTask))
            })
          }
        }
      }
    }
    case ChangeToRedCommand => {
      responses = Set()
      workers.values foreach (_ ! ChangeToRedCommand)
      val orginalSender = sender
      val timeoutTask = context.system.scheduler.scheduleOnce(timeout, self, TimeoutEvent)(context.system.dispatcher)
      context.become(receiveRedEvents(orginalSender, timeoutTask) {
        timeoutTask.cancel()
        orginalSender ! ChangedToRedEvent
        context.become(receiveWhenFree)
        unstashAll()
      })
    }
    case msg: Command => workers.values foreach (_ forward msg)
    case msg: Query   => workers.values foreach (_ forward msg)
  }

  def receiveRedEvents(originalSender: ActorRef, timeoutTask: Cancellable)(execute: => Unit): Receive = akka.event.LoggingReceive {
    case ChangedToRedEvent => {
      responses = responses + sender
      if (responses.size == workers.size) {
        timeoutTask.cancel()
        execute
      }
    }
    case TimeoutEvent => {
      throw new Exception(s"timeout waiting for all red events")
    }
    case m @ GetStatusQuery => workers.values foreach (_ forward m)
    case msg                => stash()
  }

  def receiveFinalGreenEventWhenBusy(id: String, originalSender: ActorRef, timeoutTask: Cancellable): Receive = {
    case ChangedToGreenEvent => {
      timeoutTask.cancel()
      originalSender ! ChangedToGreenEvent
      currentGreenId = id
      context.become(receiveWhenFree)
      unstashAll()
    }
    case TimeoutEvent       => throw new Exception()
    case m @ GetStatusQuery => workers.values foreach (_ forward m)
    case msg                => stash()
  }

}
