package trafficlightscontrol.http

import scala.annotation.tailrec

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
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

  val publisher = new publishers.PublisherActor[StateChangedEvent]()(context.system)

  var report: Map[Id, LightState] = Map()

  def receive = {

    case event @ StateChangedEvent(id, status) =>
      report += (id -> status)
      publisher.publish(event)
    //sendToPublishers(event)

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
      val p = publisher.withPredicate((e: StateChangedEvent) => predicate(e.id))
      sender ! p
  }

  context.system.eventStream.subscribe(self, classOf[StateChangedEvent])
}

object publishers {

  import org.reactivestreams.{ Subscriber, Publisher }

  final class PublisherActor[T](implicit system: ActorSystem) extends Publisher[T] {

    private[this] val worker = system.actorOf(Props(new PublisherActorWorker))

    override def subscribe(subscriber: Subscriber[_ >: T]): Unit = {
      Option(subscriber).map(s => new Subscription(subscriber, worker, all))
        .getOrElse(throw new NullPointerException)
    }

    def publish(element: T): Unit = worker ! Publish(element)

    case class Cancel(s: Subscription)
    case class Subscribe(s: Subscription, predicate: T => Boolean)
    case class Demand(s: Subscription, n: Long)
    case class Publish(element: T)

    final def all: T => Boolean = (e: T) => true

    final case class Subscription(subscriber: Subscriber[_ >: T], worker: ActorRef, predicate: T => Boolean, var cancelled: Boolean = false, var demand: Long = 0) extends org.reactivestreams.Subscription {
      override def cancel(): Unit = worker ! Cancel(this)
      override def request(n: Long): Unit = worker ! Demand(this, n)
      private[publishers] def push(element: T): Unit = {
        if (demand > 0) {
          subscriber.onNext(element)
          demand = demand - 1
        }
        else {
          //skip elements which cannot be pushed directly to the subscriber
        }
      }
      worker ! Subscribe(this, predicate)
    }

    private[this] final class PublisherActorWorker extends Actor {

      var subscriptions: Vector[Subscription] = Vector()

      def receive: Receive = {
        case Subscribe(s, _) =>
          subscriptions.find(_.subscriber == s.subscriber).getOrElse {
            subscriptions = subscriptions :+ s
            s.subscriber.onSubscribe(s)
          }
        case Cancel(s) if !s.cancelled =>
          subscriptions = subscriptions.filterNot(_ == s)
          s.cancelled = true
          s.demand = 0
        case Cancel(s) if s.cancelled =>
        case Demand(s, n) if !s.cancelled && n > 0 =>
          s.demand = s.demand + n
          if (s.demand < 0) s.demand = 0
        case Demand(s, n) if s.cancelled =>
        case Publish(element) =>
          subscriptions.foreach(s => if (s.predicate(element)) s.push(element))
        case _ =>
      }

    }

    def withPredicate(predicate: T => Boolean): Publisher[T] = new Publisher[T] {

      override def subscribe(subscriber: Subscriber[_ >: T]): Unit = {
        Option(subscriber).map(s => new Subscription(subscriber, worker, predicate))
          .getOrElse(throw new NullPointerException)
      }
    }

  }
}
