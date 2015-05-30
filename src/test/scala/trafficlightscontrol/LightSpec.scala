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
class LightSpec extends FlatSpecLike with Matchers with ActorSystemTestKit with TrafficSystemTestKit {

  object Ack extends Event

  "A Light" should "change status from red to green" in new ActorSystemTest {
    val tested = TestLight(initialState = RedLight)
    tested ! SetDirectorCommand(self, Some(Ack))
    expectMsg(Ack)
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", RedLight))
    tested ! ChangeToGreenCommand("1")
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", OrangeThenGreenLight))
    expectMsg(testLightChangeDelay * 2, ChangedToGreenEvent)
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", GreenLight))
  }

  it should "change status from green to red" in new ActorSystemTest {
    val tested = TestLight(initialState = GreenLight)
    tested ! SetDirectorCommand(testActor)
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", GreenLight))
    tested ! ChangeToRedCommand
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", OrangeThenRedLight))
    expectMsg(testLightChangeDelay * 2, ChangedToRedEvent)
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", RedLight))
  }

  it should "return current status" in new ActorSystemTest {
    val greenLight = TestLight(id = "A", initialState = GreenLight)
    greenLight ! SetDirectorCommand(testActor)
    val redLight = TestLight(id = "B", initialState = RedLight)
    redLight ! SetDirectorCommand(testActor)
    greenLight ! GetStatusQuery
    expectMsg(StatusEvent("A", GreenLight))
    redLight ! GetStatusQuery
    expectMsg(StatusEvent("B", RedLight))
  }

  it should "switch to red when orange light before green" in new ActorSystemTest {
    val tested = TestLight(initialState = OrangeThenGreenLight)
    tested ! SetDirectorCommand(testActor)
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", OrangeThenGreenLight))
    tested ! ChangeToGreenCommand("1")
    tested ! ChangeToRedCommand
    expectNoMsg(1.second)
    tested ! ChangeFromOrangeCommand
    tested ! GetStatusQuery
    expectMsg(ChangedToRedEvent)
    expectMsg(StatusEvent("1", RedLight))
  }

  it should "stay red when orange before red light" in new ActorSystemTest {
    val tested = TestLight(initialState = OrangeThenRedLight)
    tested ! SetDirectorCommand(testActor)
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", OrangeThenRedLight))
    tested ! ChangeToGreenCommand("1")
    tested ! ChangeToRedCommand
    expectNoMsg(1.second)
    tested ! ChangeFromOrangeCommand
    tested ! GetStatusQuery
    expectMsg(ChangedToRedEvent)
    expectMsg(StatusEvent("1", RedLight))
  }

  "A LightFSM" should "change status from red to green" in new ActorSystemTest {
    val tested = TestLightFSM(initialState = RedLight)
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", RedLight))
    tested ! ChangeToGreenCommand("1")
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", OrangeThenGreenLight))
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
    expectMsg(StatusEvent("1", OrangeThenRedLight))
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
    val tested = TestLightFSM(initialState = OrangeThenGreenLight)
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", OrangeThenGreenLight))
    tested ! ChangeToGreenCommand("1")
    tested ! ChangeToRedCommand
    expectNoMsg(1.second)
    tested ! ChangeFromOrangeCommand
    tested ! GetStatusQuery
    expectMsg(ChangedToGreenEvent)
    expectMsg(StatusEvent("1", OrangeThenRedLight))
    expectMsg(ChangedToRedEvent)
  }

}
