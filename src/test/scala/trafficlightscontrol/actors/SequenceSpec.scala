package trafficlightscontrol.actors

import trafficlightscontrol.model._

class SequenceSpec extends SequenceTestSuite with TrafficSystemTestKit {

  runSuite("Sequence", (id: String, initialState: Seq[LightState]) => TestSequence(id, initialState)(actorSystem))

}