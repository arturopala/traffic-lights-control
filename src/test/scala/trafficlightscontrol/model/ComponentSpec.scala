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
    val layout: Component = Sequence("s1", SequenceStrategy.RoundRobin,
      Group(
        "g1",
        Light("l1", RedLight),
        Light("l2", GreenLight)
      ),
      Group(
        "g2",
        Light("l3", GreenLight),
        Light("l4", RedLight)
      ))

    info("serialize layout to json")
    val json = layout.toJson.compactPrint
    json should be("""{"type":"Sequence","id":"s1","strategy":"RoundRobin","members":[{"type":"Group","id":"g1","members":[{"type":"Light","id":"l1","state":"R"},{"type":"Light","id":"l2","state":"G"}]},{"type":"Group","id":"g2","members":[{"type":"Light","id":"l3","state":"G"},{"type":"Light","id":"l4","state":"R"}]}]}""")

    info("deserialize it back as Component")
    val result = json.parseJson.convertTo[Component]
    result should be(layout)
  }

}