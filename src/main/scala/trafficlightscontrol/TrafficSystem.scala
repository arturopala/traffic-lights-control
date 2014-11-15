package trafficlightscontrol

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import scala.concurrent.duration._

class TrafficSystem extends Actor {

  val lights1: Map[String, ActorRef] = (1 to 4) map { c => (s"$c" -> context.actorOf(Props(classOf[TrafficLight], s"$c", RedLight, 1.seconds))) } toMap
  val group1: ActorRef = context.actorOf(Props(classOf[LightsGroupWithOnlyOneIsGreenStrategy], lights1, 100.millis))

  def receive = {
    case msg => group1 forward msg
  }

}
