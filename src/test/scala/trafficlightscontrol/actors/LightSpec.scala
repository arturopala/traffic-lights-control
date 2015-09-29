package trafficlightscontrol.actors

import trafficlightscontrol.model._

class LightSpec extends LightTestSuite with TrafficSystemTestKit {

  runSuite("Light", (id: String, light: LightState) => TestLight(id, light)(actorSystem))

}
