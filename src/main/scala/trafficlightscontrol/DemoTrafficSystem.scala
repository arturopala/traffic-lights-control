package trafficlightscontrol

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import scala.concurrent.duration._
import akka.actor.ActorLogging

class DemoTrafficSystem(period: FiniteDuration = 10.seconds) extends Actor with ActorLogging {

  val lightSet1: Map[String, ActorRef] = (0 to 3) map { c => (""+c -> context.actorOf(Props(classOf[LightFSM], ""+c, RedLight, period / 10, true))) } toMap
  val switch1: ActorRef = context.actorOf(Props(classOf[SwitchFSM], lightSet1, period))

  val toplevel: ActorRef = switch1

  var counter = 0

  def receive = {
    case msg: Command => toplevel forward msg
    case msg: Query   => toplevel forward msg
  }

  //context.system.scheduler.schedule(period / 10, period / 2, self, TickCommand)(context.system.dispatcher, self)

}
