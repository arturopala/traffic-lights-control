package trafficlightscontrol

import akka.actor.Actor
import akka.actor.ActorRef
import scala.collection.immutable.List
import scala.collection.immutable.Nil
import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import akka.actor.Props

object GetTrafficInfo
case class TrafficInfo(lightId: String, status: Traffic)
sealed abstract class Traffic
object HighTraffic extends Traffic
object MediumTraffic extends Traffic
object SmallTraffic extends Traffic
object NoTraffic extends Traffic
case class AllDetectorsInfo(infos: List[TrafficInfo], lights: List[StatusEvent])

class TrafficDirector(val target: ActorRef, val detectors: Set[(ActorRef, String)])(implicit executionContext: ExecutionContext) extends Actor {

  def receive = {
    case m @ GetStatusQuery => target forward m

    case GetTrafficInfo => {

      val originalSender = self
      context.actorOf(Props(new Actor() {
        var detectorsInfos: List[TrafficInfo] = Nil
        var lightStates: List[StatusEvent] = Nil

        def receive = {
          case trafficInfo: TrafficInfo => {
            detectorsInfos = detectorsInfos :+ trafficInfo
            sendInfoIfAllMessagesArrived()
          }

          case statusEvent: StatusEvent => {
            lightStates = lightStates :+ statusEvent
            sendInfoIfAllMessagesArrived()
          }

          case TimeoutEvent => originalSender ! TimeoutEvent
        }

        def sendInfoIfAllMessagesArrived() = {
          if (detectorsInfos.size == detectors.size && lightStates.size == detectors.size) {
            timeoutMessager.cancel
            originalSender ! new AllDetectorsInfo(detectorsInfos, lightStates)
          }
        }

        detectors.foreach(_._1 ! GetTrafficInfo)
        target ! GetStatusQuery

        import context.dispatcher
        val timeoutMessager = context.system.scheduler.scheduleOnce(1000 milliseconds) {
          self ! TimeoutEvent
        }
      }))
    }

    case AllDetectorsInfo(traffics: List[TrafficInfo], lights: List[StatusEvent]) => {
      println("AllDetectorsInfo. traffics: " + traffics + ", lights: " + lights)
      context.system.scheduler.scheduleOnce(1 seconds) {
        self ! GetTrafficInfo
      }
    }

    case TimeoutEvent => {
      self ! GetTrafficInfo
    }
  }

}
