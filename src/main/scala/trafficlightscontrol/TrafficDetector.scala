package trafficlightscontrol

import akka.actor.Actor

class TrafficDetector extends Actor {

  val allStates = Set(NoTraffic, SmallTraffic, MediumTraffic, HighTraffic)

  import scala.util.Random
  val rand = new Random

  def receive = {
    case GetTrafficInfo => sender ! allStates.toVector(rand.nextInt(allStates.size))
  }
}
