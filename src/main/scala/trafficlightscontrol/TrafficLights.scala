package trafficlightscontrol

import akka.actor.Actor

object TrafficLights {

  object ChangeToRedCommand
  object ChangeToGreenCommand
  object GetStatusQuery
  object ChangedToRedEvent
  object ChangedToGreenEvent
  object RedLightStatus
  object GreenLightStatus

  trait Light
  object RedLight extends Light
  object GreenLight extends Light
  object OrangeLight extends Light

}

class TrafficLight extends Actor {
  import TrafficLights._

  var status: Light = RedLight

  def receive = {
    case ChangeToRedCommand => {
      status = RedLight
      sender ! ChangedToRedEvent
    }
    case ChangeToGreenCommand => {
      status = GreenLight
      sender ! ChangedToGreenEvent
    }
  }

}
