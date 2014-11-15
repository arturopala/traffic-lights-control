package trafficlightscontrol

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import scala.concurrent.duration._
import akka.actor.ActorLogging

class TrafficSystem(period: FiniteDuration = 10.seconds) extends Actor with ActorLogging {

  object TickLightCommand

  val lights1: Map[String, ActorRef] = (0 to 3) map { c => ("" + c -> context.actorOf(Props(classOf[TrafficLight], "" + c, RedLight, period / 10))) } toMap
  val group1: ActorRef = context.actorOf(Props(classOf[LightsGroupWithOnlyOneIsGreenStrategy], lights1, period))

  var counter = 0

  def receive = {
    case TickLightCommand => {
      group1 ! ChangeToGreenCommand("" + counter)
      counter = counter + 1
      if (counter >= lights1.size) counter = 0
    }
    case msg => group1 forward msg
  }

  context.system.scheduler.schedule(period / 10, period, self, TickLightCommand)(context.system.dispatcher, self)

}
