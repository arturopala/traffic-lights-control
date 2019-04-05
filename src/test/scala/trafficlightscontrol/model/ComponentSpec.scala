package trafficlightscontrol.model

import org.scalatest.{FlatSpecLike, Matchers}
import scala.concurrent.duration._

import trafficlightscontrol.http._

class ComponentSpec extends FlatSpecLike with Matchers {

  import spray.json._
  import DefaultJsonProtocol._
  import JsonProtocol._

  implicit val configuration = Configuration.default

  "Component" should "convert to and from JSON" in {
    val layout: Component = Switch(
      Sequence(
        "s1",
        SequenceStrategy.RoundRobin,
        Group(
          "g1",
          Light("l1", RedLight),
          Light("l2", GreenLight)
        ),
        Group(
          "g2",
          Light("l3", GreenLight),
          Offset(
            500.millis,
            Light("l4", RedLight)
          )
        ),
        Pulse(
          Sequence("s2", SequenceStrategy.RoundRobin, Light("l5", GreenLight), Light("l6", RedLight))
        )
      ),
      initiallyGreen = true,
      skipTicks = 2
    )

    info("serialize layout to JSON string")
    val json = layout.toJson.compactPrint

    json should include("\"type\":\"switch\"")
    json should include("\"type\":\"light\"")
    json should include("\"type\":\"group\"")
    json should include("\"type\":\"sequence\"")
    json should include("\"type\":\"pulse\"")
    json should include("\"type\":\"offset\"")
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
    json should include("\"offset\":\"500 milliseconds\"")

    info("deserialize it back as a Component")
    val result = json.parseJson.convertTo[Component]
    result should be(layout)
  }

  "FiniteDuration" should "umarshall from valid string or number" in {

    def offsetFrom(json: String): FiniteDuration =
      json.parseJson.asJsObject.getFields("offset").head.convertTo[FiniteDuration]

    offsetFrom("{\"offset\":\"125\"}") should be(125.millis)
    offsetFrom("{\"offset\":125}") should be(125.millis)
    offsetFrom("{\"offset\":\"100 milliseconds\"}") should be(100.millis)
    offsetFrom("{\"offset\":\"100 millis\"}") should be(100.millis)
    offsetFrom("{\"offset\":\"125 ms\"}") should be(125.millis)
    offsetFrom("{\"offset\":\"100 secs\"}") should be(100.seconds)
    offsetFrom("{\"offset\":\"100.milliseconds\"}") should be(100.millis)
    offsetFrom("{\"offset\":\"100.millis\"}") should be(100.millis)
    offsetFrom("{\"offset\":\"125.ms\"}") should be(125.millis)
    offsetFrom("{\"offset\":\"100.secs\"}") should be(100.seconds)
    offsetFrom("{\"offset\":\"100milliseconds\"}") should be(100.millis)
    offsetFrom("{\"offset\":\"100millis\"}") should be(100.millis)
    offsetFrom("{\"offset\":\"125ms\"}") should be(125.millis)
    offsetFrom("{\"offset\":\"100secs\"}") should be(100.seconds)
  }

}
