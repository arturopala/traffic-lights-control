package trafficlightscontrol

class LightFSMSpec extends LightTestSuite with TrafficSystemTestKit {

  runSuite("LightFSM", (id: String, light: LightState) => TestLightFSM(id, light)(actorSystem))

}
