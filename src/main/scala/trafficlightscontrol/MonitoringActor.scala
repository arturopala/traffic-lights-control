package trafficlightscontrol

import akka.actor.Actor
import akka.actor.ActorRef
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import akka.actor.ActorPath
import akka.actor.Terminated

class MonitoringActor(target: ActorRef) extends Actor with WebSocketProducerActor {

  var report: Map[String, Light] = Map()
  var listeners: Set[ActorRef] = Set()

  def receive = {
    case GetReportQuery =>
      sender ! ReportEvent(report)

    case StatusEvent(id, status) =>
      report += (id -> status)
      val msg = s"$id:${status.id}"
      listeners foreach (l => push(l, msg))

    case ws.Open(_, origin) =>
      listeners += origin
      context watch origin

    case Terminated(origin) =>
      listeners -= origin
  }

  context.system.scheduler.scheduleOnce(1 seconds, target, RegisterMonitorCommand(self))(context.system.dispatcher)

}
