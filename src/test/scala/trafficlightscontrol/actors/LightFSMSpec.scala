package trafficlightscontrol.actors

import trafficlightscontrol.model._

class LightFSMSpec extends LightTestSuite with TrafficSystemTestKit {

  runSuite("LightFSM", (id: String, light: LightState) => TestLightFSM(id, light)(actorSystem))

}
