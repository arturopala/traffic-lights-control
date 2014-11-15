package trafficlightscontrol

import akka.actor.Actor
import akka.actor.ActorRef
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

class MonitoringActor(trafficSystem: ActorRef) extends Actor {

  var report: Map[String, Light] = Map()

  def receive = {
    case GetReportQuery => {
      sender ! ReportEvent(report)
    }
    case StatusEvent(id, status) => {
      report += (id -> status)
    }
  }

  context.system.scheduler.schedule(0.milliseconds, 1.seconds, trafficSystem, GetStatusQuery)(context.system.dispatcher, self)

}
