package trafficlightscontrol.actors

import org.scalatest.{ FlatSpecLike, Matchers }
import org.scalatest.concurrent.ScalaFutures
import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestActorRef, TestKit }
import com.typesafe.config.ConfigFactory
import akka.testkit.{ TestProbe, EventFilter, TestKitBase }
import scala.concurrent.duration._
import akka.actor.ActorRef

import trafficlightscontrol.model._
import trafficlightscontrol.actors._

class TrafficSystemSpec extends FlatSpecLike with Matchers with ScalaFutures with ActorSystemTestKit {

  implicit val config = Configuration(
    interval = 1.second,
    delayGreenToRed = 60.milliseconds,
    delayRedToGreen = 40.milliseconds,
    sequenceDelay = 10.milliseconds,
    timeout = 1.second
  )

  val strategy = SequenceStrategy.RoundRobin

  val layout = Sequence("s1", strategy,
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

  "A TrafficSystem" should "be materialized with TrafficSystemMaterializer" in new ActorSystemTest {

    val props = TrafficSystem.props(layout, "foo")(TrafficSystemMaterializer)
    val trafficSystemRef = actorSystem.actorOf(props)

    eventListener.expectMsgAllOf(
      checkTimeout,
      StatusEvent("foo_l1", RedLight),
      StatusEvent("foo_l2", GreenLight),
      StatusEvent("foo_l3", GreenLight),
      StatusEvent("foo_l4", RedLight)
    )

  }

  it should "be materialized with TrafficSystemMaterializerFSM" in new ActorSystemTest {

    val props = TrafficSystem.props(layout, "bar")(TrafficSystemMaterializerFSM)
    val trafficSystemRef = actorSystem.actorOf(props)

    eventListener.expectMsgAllOf(
      checkTimeout,
      StatusEvent("bar_l1", RedLight),
      StatusEvent("bar_l2", GreenLight),
      StatusEvent("bar_l3", GreenLight),
      StatusEvent("bar_l4", RedLight)
    )

  }

  it should "start and stop with StartSystemCommand, StopSystemCommand" in new ActorSystemTest {

    val props = TrafficSystem.props(layout, "foo1")(TrafficSystemMaterializer)
    val trafficSystemRef = actorSystem.actorOf(props)

    val probe = TestProbe()
    probe watch trafficSystemRef
    trafficSystemRef ! StartSystemCommand("foo1")
    expectMsg(SystemStartedEvent("foo1"))
    trafficSystemRef ! StopSystemCommand("foo1")
    probe.expectTerminated(trafficSystemRef)

  }

  it should "ignore duplicate StartSystemCommand" in new ActorSystemTest {

    val props = TrafficSystem.props(layout, "foo2")(TrafficSystemMaterializer)
    val trafficSystemRef = actorSystem.actorOf(props)
    trafficSystemRef ! StartSystemCommand("foo2")
    expectMsg(SystemStartedEvent("foo2"))
    trafficSystemRef ! StartSystemCommand("foo2")
    expectMsg(MessageIgnoredEvent(StartSystemCommand("foo2")))
  }

  it should "ignore premature StopSystemCommand" in new ActorSystemTest {

    val props = TrafficSystem.props(layout, "foo3")(TrafficSystemMaterializer)
    val trafficSystemRef = actorSystem.actorOf(props)
    val probe = TestProbe()
    probe watch trafficSystemRef
    trafficSystemRef ! StopSystemCommand("foo3")
    expectMsg(MessageIgnoredEvent(StopSystemCommand("foo3")))
  }

}

class TestTrafficSystemMaterializer(implicit val system: ActorSystem) extends DefaultTrafficSystemMaterializer with TestKitBase {
  val probe = TestProbe()
  override def lightProps(light: Light, systemId: Id): Props = {
    val props = LightActor.props(combineId(systemId, light.id), light.initialState, light.configuration)
    ProxyTestProps(probe, props, light.id)
  }
}
