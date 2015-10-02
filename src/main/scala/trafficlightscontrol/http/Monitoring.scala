package trafficlightscontrol.http

import scala.annotation.tailrec

import akka.actor.Actor
import akka.actor.ActorRef
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import akka.actor.ActorPath
import akka.actor.Terminated
import akka.actor.ActorLogging
import akka.actor.Props

import trafficlightscontrol.model._
import trafficlightscontrol.actors._

import akka.stream.actor._

/**
 * Actor responsible of listening on EventStream for StatusEvents. <br>
 * Keeps current system status and spreads it responding on:
 * <li>   GetReportQuery => ReportEvent
 * <li>   GetStatusQuery(id: Id) => StatusEvent
 * <li>   GetPublisherQuery(predicate: Id => Boolean) => Publisher[StatusEvent]
 */
class MonitoringActor extends Actor with ActorLogging {

  var report: Map[Id, LightState] = Map()
  var publishers: Set[(Id => Boolean, ActorRef)] = Set.empty

  def receive = {

    case event @ StatusEvent(id, status) =>
      report += (id -> status)
      sendToPublishers(event)

    case GetReportQuery =>
      sender ! ReportEvent(report)

    case GetStatusQuery(id) =>
      report.get(id) match {
        case Some(state) => sender ! Some(StatusEvent(id, state))
        case None        => sender ! None
      }

    case GetPublisherQuery(predicate) =>
      val publisherActor = context.actorOf(StatusPublisherActor.props)
      context.watch(publisherActor)
      publishers = publishers + (predicate -> publisherActor)
      val publisher = ActorPublisher(publisherActor)
      sender ! publisher

    case Terminated(publisherActor) =>
      publishers = publishers filterNot { case (_, ref) => ref == publisherActor }
  }

  def sendToPublishers(event: StatusEvent): Unit = {
    for ((p, ref) <- publishers) if (p(event.id)) ref ! event
  }

  context.system.eventStream.subscribe(self, classOf[StatusEvent])
}

case class Monitoring(actor: ActorRef)

class StatusPublisherActor extends Actor with ActorPublisher[StatusEvent] {

  import akka.stream.actor.ActorPublisherMessage._

  var eventOpt: Option[StatusEvent] = None

  def receive = {
    case event: StatusEvent =>
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

object StatusPublisherActor {
  val props: Props = Props(classOf[StatusPublisherActor])
}
