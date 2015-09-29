package trafficlightscontrol.actors

class SwitchFSMSpec extends SwitchTestSuite with TrafficSystemTestKit {

  runSuite("SwitchFSM", (id: String, initialState: Seq[LightState]) => TestSwitchFSM(id, initialState)(actorSystem))

}