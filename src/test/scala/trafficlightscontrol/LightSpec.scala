package trafficlightscontrol

class LightSpec extends LightTestSuite with TrafficSystemTestKit {

  runSuite("Light", (id: String, light: LightState) => TestLight(id, light)(actorSystem))

}
