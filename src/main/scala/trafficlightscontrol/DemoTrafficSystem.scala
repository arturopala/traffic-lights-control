package trafficlightscontrol

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import scala.concurrent.duration._
import akka.actor.ActorLogging

class DemoTrafficSystem(period: FiniteDuration = 10.seconds) extends Actor with ActorLogging {

  val lights1: Map[String, ActorRef] = (0 to 3) map { c => (""+c -> context.actorOf(Props(classOf[LightFSM], ""+c, RedLight, period / 10))) } toMap
  val group1: ActorRef = context.actorOf(Props(classOf[OnlyOneIsGreenSwitchFSM], lights1, period))
  val detectors: Set[(ActorRef, String)] = (0 to 3) map { c => (context.actorOf(Props(classOf[TrafficDetector], ""+c)), ""+c) } toSet
  val toplevel: ActorRef = context.actorOf(Props(classOf[TrafficDirector], group1, detectors, period, period / 10))

  var counter = 0

  def receive = {
    case msg: Command => toplevel forward msg
    case msg: Query   => toplevel forward msg
  }

  context.system.scheduler.schedule(period / 10, period / 2, self, TickCommand)(context.system.dispatcher, self)

}
