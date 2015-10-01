package trafficlightscontrol.http

import scala.annotation.tailrec

import akka.actor.Actor
import akka.actor.ActorRef
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import akka.actor.ActorPath
import akka.actor.Terminated
import akka.actor.ActorLogging

import trafficlightscontrol.model._
import trafficlightscontrol.actors._

import akka.stream.actor._

class MonitoringActor extends Actor with ActorLogging {

  var report: Map[String, LightState] = Map()

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

  }

  context.system.eventStream.subscribe(self, classOf[StatusEvent])

}

case class Monitoring(actor: ActorRef)

class StatusPublisherActor extends Actor with ActorPublisher[StatusEvent] {

  import akka.stream.actor.ActorPublisherMessage._

  var eventOpt: Option[StatusEvent] = None

  def receive = {
    case event @ StatusEvent(_, status) =>
      if (isActive & totalDemand > 0) {
        onNext(event)
        eventOpt = None
      }
      else eventOpt = Some(event)
    case Request(n) =>
      if (isActive && n > 0) eventOpt foreach onNext
    case Cancel =>
      context.stop(self)
  }

}
