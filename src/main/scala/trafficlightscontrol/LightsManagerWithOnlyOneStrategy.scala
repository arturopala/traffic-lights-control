package trafficlightscontrol

import akka.actor._
import akka.pattern.ask
import scala.collection._
import scala.concurrent._
import scala.concurrent.duration._

class LightsManagerWithOnlyOneStrategy(val workers: Map[String, ActorRef], timeout: FiniteDuration = 10 seconds)(implicit executionContext: ExecutionContext) extends Actor with ActorLogging with Stash {

  def receive = receiveWhenFree

  var responses: Set[ActorRef] = Set()

  def receiveWhenFree: Receive = {
    case ChangeToGreenCommand(id) => {
      workers.get(id) foreach { target: ActorRef =>
        {
          responses = Set()
          workers.values foreach (_ ! ChangeToRedCommand)
          val timeoutTask = context.system.scheduler.scheduleOnce(timeout, self, TimeoutEvent)
          val orginalSender = sender
          context.become(receiveRedEvents(sender, timeoutTask) {
            target ! ChangeToGreenCommand
            val nextTimeoutTask = context.system.scheduler.scheduleOnce(timeout, self, TimeoutEvent)
            context.become(receiveFinalGreenEventWhenBusy(id, orginalSender, target, nextTimeoutTask))
          })
        }
      }
    }
    case ChangeToRedCommand => {
      responses = Set()
      workers.values foreach (_ ! ChangeToRedCommand)
      val orginalSender = sender
      val timeoutTask = context.system.scheduler.scheduleOnce(timeout, self, TimeoutEvent)
      context.become(receiveRedEvents(orginalSender, timeoutTask) {
        timeoutTask.cancel()
        orginalSender ! ChangedToRedEvent
        context.become(receiveWhenFree)
        unstashAll()
      })
    }
  }

  def receiveRedEvents(originalSender: ActorRef, timeoutTask: Cancellable)(execute: => Unit): Receive = {
    case ChangedToRedEvent => {
      responses += sender
      if (responses.size == workers.size) {
        timeoutTask.cancel()
        execute
      }
    }
    case TimeoutEvent => {
      throw new Exception()
    }
    case msg => stash()
  }

  def receiveFinalGreenEventWhenBusy(id: String, originalSender: ActorRef, target: ActorRef, timeoutTask: Cancellable): Receive = {
    case ChangedToGreenEvent => {
      if (sender == target) {
        timeoutTask.cancel()
        originalSender ! ChangedToGreenEvent(id)
        context.become(receiveWhenFree)
        unstashAll()
      }
    }
    case TimeoutEvent => {
      throw new Exception()
    }
    case msg => stash()
  }

}
