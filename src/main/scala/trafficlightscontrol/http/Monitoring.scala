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

case class Monitoring(actor: ActorRef)

/**
 * Actor responsible of listening on EventStream for StateChangedEvents. <br>
 * Keeps current system status and spreads it responding on:
 * <li>   GetReportQuery => ReportEvent
 * <li>   GetStatusQuery(id: Id) => StateChangedEvent
 * <li>   GetPublisherQuery(predicate: Id => Boolean) => Publisher[StateChangedEvent]
 */
class MonitoringActor extends Actor with ActorLogging {

  var report: Map[Id, LightState] = Map()
  var publishers: Set[(Id => Boolean, ActorRef)] = Set.empty

  def receive = {

    case event @ StateChangedEvent(id, status) =>
      report += (id -> status)
      sendToPublishers(event)

    case GetReportQuery(system) =>
      sender ! ReportEvent(report.filterKeys(k => k.startsWith(system)))

    case GetReportQuery =>
      sender ! ReportEvent(report)

    case GetStatusQuery(id) =>
      report.get(id) match {
        case Some(state) => sender ! Some(StateChangedEvent(id, state))
        case None        => sender ! None
      }

    case GetPublisherQuery(predicate) =>
      val publisherActor = context.actorOf(StatusPublisherActor.props)
      context.watch(publisherActor)
      publishers = publishers + (predicate -> publisherActor)
      val publisher = ActorPublisher(publisherActor)
      sender ! publisher
      log.info(s"Started new status publisher ${publisherActor.path}")

    case Terminated(publisherActor) =>
      log.info(s"Terminated status publisher ${publisherActor.path}")
      publishers = publishers filterNot { case (_, ref) => ref == publisherActor }
  }

  def sendToPublishers(event: StateChangedEvent): Unit = {
    for ((p, ref) <- publishers) if (p(event.id)) ref ! event
  }

  context.system.eventStream.subscribe(self, classOf[StateChangedEvent])
}

class StatusPublisherActor extends Actor with ActorPublisher[StateChangedEvent] {

  import akka.stream.actor.ActorPublisherMessage._

  var eventOpt: Option[StateChangedEvent] = None

  def receive = {
    case event: StateChangedEvent =>
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
