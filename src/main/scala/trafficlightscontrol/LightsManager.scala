package trafficlightscontrol

import akka.actor.{ Stash, ActorRef, ActorLogging, Actor }
import scala.collection._
import scala.concurrent._
import scala.concurrent.duration._

class LightsManager(workers: Map[String, ActorRef], timeout: FiniteDuration = 1 seconds)(implicit executionContext: ExecutionContext) extends Actor with ActorLogging with Stash {

  def receive = receiveWhenFree

  var responses: Set[ActorRef] = Set()

  def receiveWhenFree: Receive = {
    case ChangeToGreenCommand(id) => {
      workers.get(id) foreach { target: ActorRef =>
        {
          responses = Set()
          workers.values foreach (_ ! ChangeToRedCommand)
          context.become(receiveRedEventsWhenBusy(sender, target))
        }
      }
    }
  }

  def receiveRedEventsWhenBusy(originalSender: ActorRef, target: ActorRef): Receive = {
    case ChangedToRedEvent => {
      responses += sender
      if (responses.size == workers.size) {
        target ! ChangeToGreenCommand
        context.system.scheduler.scheduleOnce(timeout, self, Timeout)
        context.become(receiveFinalGreenEventWhenBusy(originalSender, target))
      }
    }
    case msg => stash()
  }

  def receiveFinalGreenEventWhenBusy(originalSender: ActorRef, target: ActorRef): Receive = {
    case ChangedToGreenEvent => {
      if (sender == target) {
        originalSender ! ChangedToGreenEvent
        context.become(receiveWhenFree)
        unstashAll()
      }
    }
    case msg => stash()
  }

}