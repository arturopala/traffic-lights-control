package trafficlightscontrol.actors

import trafficlightscontrol.model._

class GroupFSMSpec extends GroupTestSuite with TrafficSystemTestKit {

  runSuite("GroupFSM", (id: String, initialState: Seq[LightState]) => TestGroupFSM(id, initialState)(actorSystem))

}
