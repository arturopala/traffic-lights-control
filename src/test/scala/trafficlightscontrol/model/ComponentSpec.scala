package trafficlightscontrol.model

import org.scalatest.{ FlatSpecLike, Matchers }
import scala.concurrent.duration._

import trafficlightscontrol.http._

class ComponentSpec extends FlatSpecLike with Matchers {

  import spray.json._
  import DefaultJsonProtocol._
  import JsonProtocol._

  implicit val configuration = Configuration.default

  "Component" should "convert to and from JSON" in {
    val layout: Component = Switch(
      "sw1",
      Sequence("s1", SequenceStrategy.RoundRobin,
        Group(
          "g1",
          Light("l1", RedLight),
          Light("l2", GreenLight)
        ),
        Group(
          "g2",
          Light("l3", GreenLight),
          Light("l4", RedLight)
        ),
        Pulse(
          "p1",
          Sequence("s2", SequenceStrategy.RoundRobin,
            Light("l5", GreenLight),
            Light("l6", RedLight))
        )),
      initiallyGreen = true,
      skipTicks = 2
    )

    info("serialize layout to JSON string")
    val json = layout.toJson.compactPrint

    json should include("\"type\":\"Switch\"")
    json should include("\"type\":\"Light\"")
    json should include("\"type\":\"Group\"")
    json should include("\"type\":\"Sequence\"")
    json should include("\"type\":\"Pulse\"")
    json should include("\"id\":\"sw1\"")
    json should include("\"id\":\"s1\"")
    json should include("\"id\":\"g1\"")
    json should include("\"id\":\"g2\"")
    json should include("\"id\":\"l1\"")
    json should include("\"id\":\"l2\"")
    json should include("\"id\":\"l3\"")
    json should include("\"id\":\"l4\"")
    json should include("\"id\":\"l5\"")
    json should include("\"id\":\"l6\"")
    json should include("\"id\":\"l6\"")
    json should include("\"id\":\"s2\"")
    json should include("\"skip\":2")
    json should include("\"initially\":true")
    json should include("\"state\":\"R\"")
    json should include("\"state\":\"G\"")
    json should include("\"strategy\":\"RoundRobin\"")

    info("deserialize it back as a Component")
    val result = json.parseJson.convertTo[Component]
    result should be(layout)
  }

}
