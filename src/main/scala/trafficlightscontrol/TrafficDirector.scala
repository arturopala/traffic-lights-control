package trafficlightscontrol

import akka.actor.Actor
import akka.actor.ActorRef

class TrafficDirector(val target: ActorRef, val detectors: Set[(ActorRef, String)]) extends Actor {

  def receive = {
    case m @ GetStatusQuery => target forward m
    case _ =>
  }

}
