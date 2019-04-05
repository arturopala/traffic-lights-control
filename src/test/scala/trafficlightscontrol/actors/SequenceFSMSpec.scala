package trafficlightscontrol.actors

import trafficlightscontrol.model._

class SequenceFSMSpec extends SequenceTestSuite with TrafficSystemTestKit {

  runSuite("SequenceFSM", (id: String, initialState: Seq[LightState]) => TestSequenceFSM(id, initialState)(actorSystem))

}
