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

  val timeout = 10.seconds
  val delay = 1.second
  val strategy = SwitchStrategy.RoundRobin

  "A TrafficSystem" should "be materialized with TrafficSystemMaterializer" in new ActorSystemTest {

    val layout = Switch("s1", strategy, timeout, Seq(
      Group("g1", timeout, Seq(
        Light("l1", RedLight, delay),
        Light("l2", GreenLight, delay)
      )),
      Group("g2", timeout, Seq(
        Light("l3", GreenLight, delay),
        Light("l4", RedLight, delay)
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

    val layout = Switch("s1", strategy, timeout, Seq(
      Group("g1", timeout, Seq(
        Light("l1", RedLight, delay),
        Light("l2", GreenLight, delay)
      )),
      Group("g2", timeout, Seq(
        Light("l3", GreenLight, delay),
        Light("l4", RedLight, delay)
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
