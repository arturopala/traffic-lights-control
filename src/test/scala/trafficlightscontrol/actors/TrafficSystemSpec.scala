package trafficlightscontrol.actors

import org.scalatest.{ FlatSpecLike, Matchers }
import org.scalatest.concurrent.ScalaFutures
import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestActorRef, TestKit }
import com.typesafe.config.ConfigFactory
import akka.testkit.{ TestProbe, EventFilter }
import scala.concurrent.duration._
import akka.actor.ActorRef

import trafficlightscontrol.model._
import trafficlightscontrol.dsl._
import trafficlightscontrol.actors._

class TrafficSystemSpec extends FlatSpecLike with Matchers with ScalaFutures with ActorSystemTestKit {

  val config = Configuration(interval = 10.seconds, delayGreenToRed = 1.second, delayRedToGreen = 1.second, switchDelay = 100.millis, timeout = 10.seconds)
  val strategy = SwitchStrategy.RoundRobin

  "A TrafficSystem" should "be materialized with TrafficSystemMaterializer" in new ActorSystemTest {

    val layout = Switch("s1", strategy, config, Seq(
      Group("g1", config, Seq(
        Light("l1", RedLight, config),
        Light("l2", GreenLight, config)
      )),
      Group("g2", config, Seq(
        Light("l3", GreenLight, config),
        Light("l4", RedLight, config)
      ))
    ))

    val props = TrafficSystem.props(layout)(TrafficSystemMaterializer)
    val trafficSystemRef = actorSystem.actorOf(props)

    eventListener.expectMsgAllOf(checkTimeout,
      StatusEvent("l1", RedLight),
      StatusEvent("l2", GreenLight),
      StatusEvent("l3", GreenLight),
      StatusEvent("l4", RedLight)
    )

  }

  "A TrafficSystem" should "be materialized with TrafficSystemMaterializerFSM" in new ActorSystemTest {

    val layout = Switch("s1", strategy, config, Seq(
      Group("g1", config, Seq(
        Light("l1", RedLight, config),
        Light("l2", GreenLight, config)
      )),
      Group("g2", config, Seq(
        Light("l3", GreenLight, config),
        Light("l4", RedLight, config)
      ))
    ))

    val props = TrafficSystem.props(layout)(TrafficSystemMaterializerFSM)
    val trafficSystemRef = actorSystem.actorOf(props)

    eventListener.expectMsgAllOf(checkTimeout,
      StatusEvent("l1", RedLight),
      StatusEvent("l2", GreenLight),
      StatusEvent("l3", GreenLight),
      StatusEvent("l4", RedLight)
    )

  }

}
