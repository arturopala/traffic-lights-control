package trafficlightscontrol.actors

class GroupSpec extends GroupTestSuite with TrafficSystemTestKit {

  runSuite("Group", (id: String, initialState: Seq[LightState]) => TestGroup(id, initialState)(actorSystem))

}