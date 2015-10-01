package trafficlightscontrol.http

import akka.actor.Actor
import akka.actor.ActorRef
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import akka.actor.ActorPath
import akka.actor.Terminated
import akka.actor.ActorLogging

import trafficlightscontrol.model._
import trafficlightscontrol.actors._

class MonitoringActor extends Actor /*with WebSocketProducerActor*/ with ActorLogging {

  var report: Map[String, LightState] = Map()
  val listeners: scala.collection.mutable.Set[ActorRef] = scala.collection.mutable.Set()

  def receive = {
    case GetReportQuery =>
      sender ! ReportEvent(report)

    case GetStatusQuery(id) =>
      report.get(id) match {
        case Some(state) => sender ! Some(StatusEvent(id, state))
        case None        => sender ! None
      }

    case event @ StatusEvent(id, status) =>
      report += (id -> status)
      val msg = s"$id:${status.id}"
    //listeners foreach (l => push(l, msg))

    /*case ws.Open(_, origin) =>
      listeners add origin
      context watch origin*/

    case Terminated(origin) =>
      listeners remove origin
  }

  context.system.eventStream.subscribe(self, classOf[StatusEvent])
}

case class Monitoring(actor: ActorRef)
