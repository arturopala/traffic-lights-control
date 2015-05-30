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
class SwitchFSMSpec extends FlatSpecLike with Matchers with ActorSystemTestKit with TrafficSystemTestKit {

  "A SwitchFSM" should "change status of LightState#1 from red to green" in new ActorSystemTest {
    val tested = TestSwitchFSM(Seq(RedLight, RedLight, RedLight, RedLight))
    tested ! ChangeToGreenCommand("1")
    expectMsg(ChangedToGreenEvent)
  }

  it should "change status of LightState#1 from red to green and status of others to red" in new ActorSystemTest {
    val tested = TestSwitchFSM(Seq(RedLight, GreenLight, RedLight, RedLight))
    val lights = tested.underlyingActor.subordinates
    stateOfLightFSM(lights("1")) should equal(RedLight)
    stateOfLightFSM(lights("2")) should equal(GreenLight)
    stateOfLightFSM(lights("3")) should equal(RedLight)
    stateOfLightFSM(lights("4")) should equal(RedLight)

    tested ! ChangeToGreenCommand("1")

    expectMsg(ChangedToGreenEvent)
    expectNoMsg(200.millis)
    stateOfLightFSM(lights("1")) should equal(GreenLight)
    stateOfLightFSM(lights("2")) should equal(RedLight)
    stateOfLightFSM(lights("3")) should equal(RedLight)
    stateOfLightFSM(lights("4")) should equal(RedLight)
  }

  it should "change status of LightState#1 from red to green and status of others to red (2)" in new ActorSystemTest {
    val tested = TestSwitchFSM(Seq(RedLight, GreenLight, GreenLight, GreenLight))
    val lights = tested.underlyingActor.subordinates
    stateOfLightFSM(lights("1")) should equal(RedLight)
    stateOfLightFSM(lights("2")) should equal(GreenLight)
    stateOfLightFSM(lights("3")) should equal(GreenLight)
    stateOfLightFSM(lights("4")) should equal(GreenLight)

    tested ! ChangeToGreenCommand("1")

    expectMsg(ChangedToGreenEvent)
    expectNoMsg(200.millis)
    stateOfLightFSM(lights("1")) should equal(GreenLight)
    stateOfLightFSM(lights("2")) should equal(RedLight)
    stateOfLightFSM(lights("3")) should equal(RedLight)
    stateOfLightFSM(lights("4")) should equal(RedLight)
  }

  it should "change status of all lights to red" in new ActorSystemTest {
    val tested = TestSwitchFSM(Seq(GreenLight, GreenLight, RedLight, GreenLight))
    val lights = tested.underlyingActor.subordinates
    stateOfLightFSM(lights("1")) should equal(GreenLight)
    stateOfLightFSM(lights("2")) should equal(GreenLight)
    stateOfLightFSM(lights("3")) should equal(RedLight)
    stateOfLightFSM(lights("4")) should equal(GreenLight)

    tested ! ChangeToRedCommand

    expectMsg(ChangedToRedEvent)
    expectNoMsg(200.millis)
    stateOfLightFSM(lights("1")) should equal(RedLight)
    stateOfLightFSM(lights("2")) should equal(RedLight)
    stateOfLightFSM(lights("3")) should equal(RedLight)
    stateOfLightFSM(lights("4")) should equal(RedLight)
  }

}
