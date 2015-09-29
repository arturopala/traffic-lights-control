package trafficlightscontrol.actors

import trafficlightscontrol.model._

class SwitchSpec extends SwitchTestSuite with TrafficSystemTestKit {

  runSuite("Switch", (id: String, initialState: Seq[LightState]) => TestSwitch(id, initialState)(actorSystem))

}