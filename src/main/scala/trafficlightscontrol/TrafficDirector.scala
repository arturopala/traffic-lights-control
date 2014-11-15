package trafficlightscontrol

import akka.actor.Actor
import akka.actor.ActorRef
import scala.collection.immutable.List
import scala.collection.immutable.Nil
import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import akka.actor.Props
import akka.actor.ActorLogging

object GetTrafficInfoCommand extends Command
case class TrafficInfo(lightId: String, status: Traffic)
sealed abstract class Traffic
object HighTraffic extends Traffic
object MediumTraffic extends Traffic
object SmallTraffic extends Traffic
object NoTraffic extends Traffic
case class AllDetectorsInfo(infos: List[TrafficInfo], lights: List[StatusEvent])

class TrafficDirector(val target: ActorRef, val detectors: Set[(ActorRef, String)], period: FiniteDuration = 1 seconds, timeout: FiniteDuration = 1 seconds) extends Actor with ActorLogging {

  def receive = {
    case TickCommand => {
      val originalSender = sender
      val me = self
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

          case TimeoutEvent => {
            log.error("timeout")
          }
        }

        def sendInfoIfAllMessagesArrived() = {
          if (detectorsInfos.size == detectors.size && lightStates.size == detectors.size) {
            timeoutMessager.cancel
            me ! new AllDetectorsInfo(detectorsInfos, lightStates)
          }
        }

        detectors.foreach(_._1 ! GetTrafficInfoCommand)
        target ! GetStatusQuery

        import context.dispatcher
        val timeoutMessager = context.system.scheduler.scheduleOnce(timeout) {
          self ! TimeoutEvent
        }
      }))
    }

    case AllDetectorsInfo(traffics: List[TrafficInfo], lights: List[StatusEvent]) => {
      traffics.find(_.status == HighTraffic) foreach { ti => target ! ChangeToGreenCommand("" + ti.lightId) }
    }

    case msg: Command => target forward msg
    case msg: Query => target forward msg
  }

}
