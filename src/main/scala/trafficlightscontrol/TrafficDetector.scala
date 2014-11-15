package trafficlightscontrol

import akka.actor.Actor
import akka.actor.ActorLogging

class TrafficDetector(lightId: String) extends Actor with ActorLogging {

  val allStates = Set(NoTraffic, SmallTraffic, MediumTraffic, HighTraffic)

  import scala.util.Random
  val rand = new Random

  def receive = {
    case GetTrafficInfoCommand => {
      val id = rand.nextInt(allStates.size)
      sender ! TrafficInfo(lightId, allStates.toVector(id))
    }
  }
}
