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
import trafficlightscontrol.actors._

class TrafficSystemsManagerSpec extends FlatSpecLike with Matchers with ScalaFutures with ActorSystemTestKit {

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

  "A TrafficSystemsManager" should "receive InstallComponentCommand and install component" in new ActorSystemTest {

    val manager = actorSystem.actorOf(TrafficSystemsManager.props())

    info("install component")
    manager ! InstallComponentCommand(layout, "foo")
    expectMsg(InstallComponentSucceededEvent(layout, "foo"))

    info("check status")
    manager ! SystemInfoQuery("foo")
    val status = expectMsgType[SystemInfoEvent]
    status.system shouldBe "foo"
    status.component shouldBe layout
    status.interval shouldBe config.interval
    status.history.events should have size 1
    status.history.events.head shouldBe a[HistoryEvent.Installed]
  }

  it should "receive SystemInfoQuery and return MessageIgnoredEvent for non-existent system" in new ActorSystemTest {
    val manager = actorSystem.actorOf(TrafficSystemsManager.props())
    manager ! SystemInfoQuery("foo1")
    expectMsg(MessageIgnoredEvent(SystemInfoQuery("foo1")))
  }

  it should "receive StartSystemCommand and start system" in new ActorSystemTest {
    val manager = actorSystem.actorOf(TrafficSystemsManager.props())
    info("install component")
    manager ! InstallComponentCommand(layout, "foo1")
    expectMsg(InstallComponentSucceededEvent(layout, "foo1"))
    info("start component")
    manager ! StartSystemCommand("foo1")
    eventListener.receiveN(4, checkTimeout)
    info("check status")
    Thread.sleep(200)
    manager ! SystemInfoQuery("foo1")
    val status = expectMsgType[SystemInfoEvent]
    status.system shouldBe "foo1"
    status.component shouldBe layout
    status.interval shouldBe config.interval
    status.history.events should have size 2
    status.history.events(0) shouldBe a[HistoryEvent.Started]
    status.history.events(1) shouldBe a[HistoryEvent.Installed]
  }

  it should "receive StopSystemCommand and stop system" in new ActorSystemTest {
    val manager = actorSystem.actorOf(TrafficSystemsManager.props())
    info("install component")
    manager ! InstallComponentCommand(layout, "foo2")
    expectMsg(InstallComponentSucceededEvent(layout, "foo2"))
    info("start component")
    manager ! StartSystemCommand("foo2")
    eventListener.receiveN(4, checkTimeout)
    info("stop component")
    manager ! StopSystemCommand("foo2")
    info("check status")
    Thread.sleep(200)
    manager ! SystemInfoQuery("foo2")
    val status = expectMsgType[SystemInfoEvent]
    status.system shouldBe "foo2"
    status.component shouldBe layout
    status.interval shouldBe config.interval
    status.history.events should have size 3
    status.history.events(0) shouldBe a[HistoryEvent.Terminated]
    status.history.events(1) shouldBe a[HistoryEvent.Started]
    status.history.events(2) shouldBe a[HistoryEvent.Installed]
  }

  it should "stop and then start system again" in new ActorSystemTest {
    val manager = actorSystem.actorOf(TrafficSystemsManager.props())
    info("install component")
    manager ! InstallComponentCommand(layout, "foo3")
    expectMsg(InstallComponentSucceededEvent(layout, "foo3"))
    info("start component")
    manager ! StartSystemCommand("foo3")
    eventListener.receiveN(4, checkTimeout)
    info("stop component")
    manager ! StopSystemCommand("foo3")
    Thread.sleep(200)
    info("start component again")
    manager ! StartSystemCommand("foo3")
    info("check status")
    Thread.sleep(200)
    manager ! SystemInfoQuery("foo3")
    val status = expectMsgType[SystemInfoEvent]
    status.system shouldBe "foo3"
    status.component shouldBe layout
    status.interval shouldBe config.interval
    status.history.events should have size 4
    status.history.events(0) shouldBe a[HistoryEvent.Started]
    status.history.events(1) shouldBe a[HistoryEvent.Terminated]
    status.history.events(2) shouldBe a[HistoryEvent.Started]
    status.history.events(3) shouldBe a[HistoryEvent.Installed]
  }

  it should "receive StartSystemCommand and report SystemStartFailureEvent for non-existent system" in new ActorSystemTest {
    val manager = actorSystem.actorOf(TrafficSystemsManager.props())
    info("try start component")
    manager ! StartSystemCommand("foo2")
    expectMsgType[SystemStartFailureEvent]
  }

  it should "receive StartSystemCommand and report SystemStartFailureEvent for already running system" in new ActorSystemTest {
    val manager = actorSystem.actorOf(TrafficSystemsManager.props())
    info("install component")
    manager ! InstallComponentCommand(layout, "foo1")
    expectMsg(InstallComponentSucceededEvent(layout, "foo1"))
    info("start component")
    manager ! StartSystemCommand("foo1")
    info("try start component again")
    Thread.sleep(200)
    manager ! StartSystemCommand("foo1")
    expectMsgType[SystemStartFailureEvent]
  }

  it should "receive StopSystemCommand and report SystemStopFailureEvent for non-existent system" in new ActorSystemTest {
    val manager = actorSystem.actorOf(TrafficSystemsManager.props())
    info("try stop component")
    manager ! StopSystemCommand("foo2")
    expectMsgType[SystemStopFailureEvent]
  }

  it should "receive StopSystemCommand and report SystemStopFailureEvent for installed bu not yet started system" in new ActorSystemTest {
    val manager = actorSystem.actorOf(TrafficSystemsManager.props())
    info("install component")
    manager ! InstallComponentCommand(layout, "foo1")
    expectMsg(InstallComponentSucceededEvent(layout, "foo1"))
    info("try stop component")
    manager ! StopSystemCommand("foo1")
    expectMsgType[SystemStopFailureEvent]
  }

}
