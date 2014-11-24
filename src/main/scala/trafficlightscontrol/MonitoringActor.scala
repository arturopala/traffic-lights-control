package trafficlightscontrol

import akka.actor.Actor
import akka.actor.ActorRef
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import akka.actor.ActorPath

class MonitoringActor(target: ActorRef, period: FiniteDuration = 1.seconds) extends Actor {

  var report: Map[String, Light] = Map()

  def receive = {
    case GetReportQuery => {
      sender ! ReportEvent(report)
      target ! RegisterMonitorCommand(self)
    }
    case StatusEvent(id, status) => {
      report += (id -> status)
    }
  }

}
