package trafficlightscontrol

import akka.actor._
import akka.pattern.ask
import scala.collection._
import scala.concurrent._
import scala.concurrent.duration._

class OnlyOneIsGreenSwitch(val workers: Map[String, ActorRef], timeout: FiniteDuration = 10 seconds) extends Actor with ActorLogging with Stash {

  def receive = receiveWhenFree

  var responses: Set[ActorRef] = Set()
  var currentGreenId: String = ""

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

  def receiveRedEvents(originalSender: ActorRef, timeoutTask: Cancellable)(execute: => Unit): Receive = {
    case ChangedToRedEvent => {
      responses += sender
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
    case ChangedToGreenEvent(targetId) => {
      if (targetId == id) {
        timeoutTask.cancel()
        originalSender ! ChangedToGreenEvent(id)
        currentGreenId = id
        context.become(receiveWhenFree)
        unstashAll()
      }
      else {
        throw new Exception(s"expected green event from $targetId but received from $id")
      }
    }
    case TimeoutEvent => {
      throw new Exception()
    }
    case m @ GetStatusQuery => workers.values foreach (_ forward m)
    case msg                => stash()
  }

}
