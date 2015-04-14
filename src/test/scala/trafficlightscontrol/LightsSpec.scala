package trafficlightscontrol

import org.junit.runner.RunWith
import org.scalatest.{ FlatSpecLike, Matchers }
import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestActorRef, TestKit }
import com.typesafe.config.ConfigFactory
import akka.testkit.TestProbe
import scala.concurrent.duration._
import akka.actor.ActorRef
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class LightsSpec extends FlatSpecLike with Matchers with ActorSystemTestKit with TrafficSystemTestKit {

  "A Light" should "change status from red to green" in new ActorSystemTest {
    val tested = TestLight(initialState = RedLight)
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", RedLight))
    tested ! ChangeToGreenCommand("1")
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", OrangeLight))
    expectMsg(testLightChangeDelay * 2, ChangedToGreenEvent)
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", GreenLight))
  }

  it should "change status from green to red" in new ActorSystemTest {
    val tested = TestLight(initialState = GreenLight)
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", GreenLight))
    tested ! ChangeToRedCommand
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", OrangeLight))
    expectMsg(testLightChangeDelay * 2, ChangedToRedEvent)
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", RedLight))
  }

  it should "return current status" in new ActorSystemTest {
    val greenLight = TestLight(id = "A", initialState = GreenLight)
    val redLight = TestLight(id = "B", initialState = RedLight)
    greenLight ! GetStatusQuery
    expectMsg(StatusEvent("A", GreenLight))
    redLight ! GetStatusQuery
    expectMsg(StatusEvent("B", RedLight))
  }

  "A LightFSM" should "change status from red to green" in new ActorSystemTest {
    val tested = TestLightFSM(initialState = RedLight)
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", RedLight))
    tested ! ChangeToGreenCommand("1")
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", OrangeLight))
    expectMsg(testLightChangeDelay * 2, ChangedToGreenEvent)
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", GreenLight))
  }

  it should "change status from green to red" in new ActorSystemTest {
    val tested = TestLightFSM(initialState = GreenLight)
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", GreenLight))
    tested ! ChangeToRedCommand
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", OrangeLight))
    expectMsg(testLightChangeDelay * 2, ChangedToRedEvent)
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", RedLight))
  }

  it should "reply current state event" in new ActorSystemTest {
    val greenLight = TestLightFSM(id = "A", initialState = GreenLight)
    val redLight = TestLightFSM(id = "B", initialState = RedLight)
    greenLight ! GetStatusQuery
    expectMsg(StatusEvent("A", GreenLight))
    redLight ! GetStatusQuery
    expectMsg(StatusEvent("B", RedLight))
  }

  it should "stash commands when orange light" in new ActorSystemTest {
    val tested = TestLightFSM(initialState = OrangeLight)
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", OrangeLight))
    tested ! ChangeToGreenCommand("1")
    tested ! ChangeToRedCommand
    expectNoMsg(1.second)
    tested ! ChangeFromOrangeToGreenCommand
    tested ! GetStatusQuery
    expectMsg(ChangedToGreenEvent)
    expectMsg(StatusEvent("1", OrangeLight))
    expectMsg(ChangedToRedEvent)
  }

  "A Switch" should "change status of LightState#1 from red to green" in new ActorSystemTest {
    val tested = TestSwitch(Seq(RedLight, RedLight, RedLight, RedLight))
    tested ! ChangeToGreenCommand("1")
    expectMsg(ChangedToGreenEvent)
  }

  it should "change status of LightState#1 from red to green and status of others to red" in new ActorSystemTest {
    val tested = TestSwitch(Seq(RedLight, GreenLight, RedLight, RedLight))
    val workers = tested.underlyingActor.workers
    stateOfLight(workers("1")) should equal(RedLight)
    stateOfLight(workers("2")) should equal(GreenLight)
    stateOfLight(workers("3")) should equal(RedLight)
    stateOfLight(workers("4")) should equal(RedLight)

    tested ! ChangeToGreenCommand("1")

    expectMsg(ChangedToGreenEvent)
    stateOfLight(workers("1")) should equal(GreenLight)
    stateOfLight(workers("2")) should equal(RedLight)
    stateOfLight(workers("3")) should equal(RedLight)
    stateOfLight(workers("4")) should equal(RedLight)
  }

  it should "change status of LightState#1 from red to green and status of others to red (2)" in new ActorSystemTest {
    val tested = TestSwitch(Seq(RedLight, GreenLight, GreenLight, GreenLight))
    val workers = tested.underlyingActor.workers
    stateOfLight(workers("1")) should equal(RedLight)
    stateOfLight(workers("2")) should equal(GreenLight)
    stateOfLight(workers("3")) should equal(GreenLight)
    stateOfLight(workers("4")) should equal(GreenLight)

    tested ! ChangeToGreenCommand("1")

    expectMsg(ChangedToGreenEvent)
    stateOfLight(workers("1")) should equal(GreenLight)
    stateOfLight(workers("2")) should equal(RedLight)
    stateOfLight(workers("3")) should equal(RedLight)
    stateOfLight(workers("4")) should equal(RedLight)
  }

  it should "change status of all lights to red" in new ActorSystemTest {
    val tested = TestSwitch(Seq(GreenLight, GreenLight, RedLight, GreenLight))
    val workers = tested.underlyingActor.workers
    stateOfLight(workers("1")) should equal(GreenLight)
    stateOfLight(workers("2")) should equal(GreenLight)
    stateOfLight(workers("3")) should equal(RedLight)
    stateOfLight(workers("4")) should equal(GreenLight)

    tested ! ChangeToRedCommand

    expectMsg(ChangedToRedEvent)
    stateOfLight(workers("1")) should equal(RedLight)
    stateOfLight(workers("2")) should equal(RedLight)
    stateOfLight(workers("3")) should equal(RedLight)
    stateOfLight(workers("4")) should equal(RedLight)
  }

  "A SwitchFSM" should "change status of LightState#1 from red to green" in new ActorSystemTest {
    val tested = TestSwitchFSM(Seq(RedLight, RedLight, RedLight, RedLight))
    tested ! ChangeToGreenCommand("1")
    expectMsg(ChangedToGreenEvent)
  }

  it should "change status of LightState#1 from red to green and status of others to red" in new ActorSystemTest {
    val tested = TestSwitchFSM(Seq(RedLight, GreenLight, RedLight, RedLight))
    val workers = tested.underlyingActor.members
    stateOfLightFSM(workers("1")) should equal(RedLight)
    stateOfLightFSM(workers("2")) should equal(GreenLight)
    stateOfLightFSM(workers("3")) should equal(RedLight)
    stateOfLightFSM(workers("4")) should equal(RedLight)

    tested ! ChangeToGreenCommand("1")

    expectMsg(ChangedToGreenEvent)
    expectNoMsg(200.millis)
    stateOfLightFSM(workers("1")) should equal(GreenLight)
    stateOfLightFSM(workers("2")) should equal(RedLight)
    stateOfLightFSM(workers("3")) should equal(RedLight)
    stateOfLightFSM(workers("4")) should equal(RedLight)
  }

  it should "change status of LightState#1 from red to green and status of others to red (2)" in new ActorSystemTest {
    val tested = TestSwitchFSM(Seq(RedLight, GreenLight, GreenLight, GreenLight))
    val workers = tested.underlyingActor.members
    stateOfLightFSM(workers("1")) should equal(RedLight)
    stateOfLightFSM(workers("2")) should equal(GreenLight)
    stateOfLightFSM(workers("3")) should equal(GreenLight)
    stateOfLightFSM(workers("4")) should equal(GreenLight)

    tested ! ChangeToGreenCommand("1")

    expectMsg(ChangedToGreenEvent)
    expectNoMsg(200.millis)
    stateOfLightFSM(workers("1")) should equal(GreenLight)
    stateOfLightFSM(workers("2")) should equal(RedLight)
    stateOfLightFSM(workers("3")) should equal(RedLight)
    stateOfLightFSM(workers("4")) should equal(RedLight)
  }

  it should "change status of all lights to red" in new ActorSystemTest {
    val tested = TestSwitchFSM(Seq(GreenLight, GreenLight, RedLight, GreenLight))
    val workers = tested.underlyingActor.members
    stateOfLightFSM(workers("1")) should equal(GreenLight)
    stateOfLightFSM(workers("2")) should equal(GreenLight)
    stateOfLightFSM(workers("3")) should equal(RedLight)
    stateOfLightFSM(workers("4")) should equal(GreenLight)

    tested ! ChangeToRedCommand

    expectMsg(ChangedToRedEvent)
    expectNoMsg(200.millis)
    stateOfLightFSM(workers("1")) should equal(RedLight)
    stateOfLightFSM(workers("2")) should equal(RedLight)
    stateOfLightFSM(workers("3")) should equal(RedLight)
    stateOfLightFSM(workers("4")) should equal(RedLight)
  }

}
