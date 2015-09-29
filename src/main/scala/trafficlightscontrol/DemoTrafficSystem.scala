package trafficlightscontrol

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import scala.concurrent.duration._
import akka.actor.ActorLogging

import trafficlightscontrol.model._
import trafficlightscontrol.actors._
import trafficlightscontrol.dsl._

class DemoTrafficSystem(interval: FiniteDuration = 5.seconds) extends Actor with ActorLogging {

  val delay = interval / 5
  val timeout = interval * 10

  val layout = Switch("s1", SwitchStrategy.RoundRobin, timeout, Seq(
    Group("g1", timeout, Seq(
      Light("l1", RedLight, delay),
      Light("l2", GreenLight, delay)
    )),
    Group("g2", timeout, Seq(
      Light("l3", GreenLight, delay),
      Light("l4", RedLight, delay)
    ))
  ))

  val props = TrafficSystem.props(layout)(TrafficSystemMaterializer)
  val trafficSystem = context.actorOf(props)

  def receive: Receive = {
    case "Start" =>
      context.system.scheduler.schedule(delay, interval, trafficSystem, ChangeToGreenCommand)(context.system.dispatcher, self)
  }

}
