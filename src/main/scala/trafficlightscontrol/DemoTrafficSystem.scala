package trafficlightscontrol

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import scala.concurrent.duration._
import akka.actor.ActorLogging

import trafficlightscontrol.model._
import trafficlightscontrol.actors._
import trafficlightscontrol.dsl._

class DemoTrafficSystem(interval: FiniteDuration) extends Actor with ActorLogging {

  val config = Configuration(
    interval = interval,
    delayRedToGreen = interval / 4,
    delayGreenToRed = interval / 6,
    switchDelay = interval / 10,
    timeout = interval * 10
  )

  val layout = Switch("s1", SwitchStrategy.RoundRobin, config, Seq(
    Group("g1", config, Seq(
      Light("l1", RedLight, config),
      Light("l2", GreenLight, config)
    )),
    Group("g2", config, Seq(
      Light("l3", GreenLight, config),
      Light("l4", RedLight, config)
    ))
  ))

  val props = TrafficSystem.props(layout)(TrafficSystemMaterializer)
  val trafficSystem = context.actorOf(props)

  val separator = "-" * 60

  def receive: Receive = {
    case "Start" =>
      context.system.scheduler.schedule(config.delayRedToGreen, interval, self, "Tick")(context.system.dispatcher, self)
    case "Tick" =>
      log.info(separator)
      trafficSystem ! ChangeToGreenCommand
  }

}
