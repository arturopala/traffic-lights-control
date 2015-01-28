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
class TrafficLightsSpec extends FlatSpecLike with Matchers with ActorSystemTestKit with TrafficSystemTestKit {

  "A TrafficLight" should "change status from red to green" in new ActorSystemTest {
    val tested = TestTrafficLight(initialState = RedLight)
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", RedLight))
    tested ! ChangeToGreenCommand("1")
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", OrangeLight))
    expectMsg(testLightChangeDelay * 2, ChangedToGreenEvent("1"))
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", GreenLight))
  }

  it should "change status from green to red" in new ActorSystemTest {
    val tested = TestTrafficLight(initialState = GreenLight)
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
    val greenLight = TestTrafficLight(id = "A", initialState = GreenLight)
    val redLight = TestTrafficLight(id = "B", initialState = RedLight)
    greenLight ! GetStatusQuery
    expectMsg(StatusEvent("A", GreenLight))
    redLight ! GetStatusQuery
    expectMsg(StatusEvent("B", RedLight))
  }

  "A OnlyOneIsGreenSwitch" should "change status of Light#1 from red to green" in new ActorSystemTest {
    val tested = TestLightManager(Seq(RedLight, RedLight, RedLight, RedLight))
    tested ! ChangeToGreenCommand("1")
    expectMsg(ChangedToGreenEvent("1"))
  }

  it should "change status of Light#1 from red to green and status of others to red" in new ActorSystemTest {
    val tested = TestLightManager(Seq(RedLight, GreenLight, RedLight, RedLight))
    val workers = tested.underlyingActor.workers
    stateOfTrafficLight(workers("1")) should equal(RedLight)
    stateOfTrafficLight(workers("2")) should equal(GreenLight)
    stateOfTrafficLight(workers("3")) should equal(RedLight)
    stateOfTrafficLight(workers("4")) should equal(RedLight)

    tested ! ChangeToGreenCommand("1")

    expectMsg(ChangedToGreenEvent("1"))
    stateOfTrafficLight(workers("1")) should equal(GreenLight)
    stateOfTrafficLight(workers("2")) should equal(RedLight)
    stateOfTrafficLight(workers("3")) should equal(RedLight)
    stateOfTrafficLight(workers("4")) should equal(RedLight)
  }

  it should "change status of Light#1 from red to green and status of others to red (2)" in new ActorSystemTest {
    val tested = TestLightManager(Seq(RedLight, GreenLight, GreenLight, GreenLight))
    val workers = tested.underlyingActor.workers
    stateOfTrafficLight(workers("1")) should equal(RedLight)
    stateOfTrafficLight(workers("2")) should equal(GreenLight)
    stateOfTrafficLight(workers("3")) should equal(GreenLight)
    stateOfTrafficLight(workers("4")) should equal(GreenLight)

    tested ! ChangeToGreenCommand("1")

    expectMsg(ChangedToGreenEvent("1"))
    stateOfTrafficLight(workers("1")) should equal(GreenLight)
    stateOfTrafficLight(workers("2")) should equal(RedLight)
    stateOfTrafficLight(workers("3")) should equal(RedLight)
    stateOfTrafficLight(workers("4")) should equal(RedLight)
  }

  it should "change status of all lights to red" in new ActorSystemTest {
    val tested = TestLightManager(Seq(GreenLight, GreenLight, RedLight, GreenLight))
    val workers = tested.underlyingActor.workers
    stateOfTrafficLight(workers("1")) should equal(GreenLight)
    stateOfTrafficLight(workers("2")) should equal(GreenLight)
    stateOfTrafficLight(workers("3")) should equal(RedLight)
    stateOfTrafficLight(workers("4")) should equal(GreenLight)

    tested ! ChangeToRedCommand

    expectMsg(ChangedToRedEvent)
    stateOfTrafficLight(workers("1")) should equal(RedLight)
    stateOfTrafficLight(workers("2")) should equal(RedLight)
    stateOfTrafficLight(workers("3")) should equal(RedLight)
    stateOfTrafficLight(workers("4")) should equal(RedLight)
  }

}
