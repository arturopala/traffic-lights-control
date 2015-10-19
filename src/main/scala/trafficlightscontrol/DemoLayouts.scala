package trafficlightscontrol

import scala.concurrent.duration._
import trafficlightscontrol.model._

object DemoLayouts {

  val interval = 10.seconds

  implicit val config = Configuration(
    interval = interval,
    delayRedToGreen = interval / 4,
    delayGreenToRed = interval / 6,
    sequenceDelay = interval / 10,
    timeout = interval * 10
  )

  val demoLayout =

    Sequence("s1", SequenceStrategy.RoundRobin,
      Group(
        "g1",
        Light("l1", RedLight),
        Offset(
          500.millis,
          Light("l2", GreenLight)
        )
      ),
      Group(
        "g2",
        Light("l3", GreenLight),
        Offset(
          1.second,
          Light("l4", RedLight)
        )
      ),
      Group(
        "g3",
        Light("l5", GreenLight),
        Offset(
          1.second,
          Light("l6", RedLight)
        ),
        Sequence("s2", SequenceStrategy.RoundRobin,
          Light("l7", RedLight),
          Light("l8", RedLight),
          Light("l9", RedLight))
      ))

  val layouts = Map(
    "demo" -> demoLayout
  )

}