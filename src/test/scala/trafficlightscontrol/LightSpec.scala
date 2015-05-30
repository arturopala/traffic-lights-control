package trafficlightscontrol

import org.junit.runner.RunWith
import org.scalatest.{ FlatSpecLike, Matchers }
import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestActorRef, TestKit }
import com.typesafe.config.ConfigFactory
import akka.testkit.TestProbe
import scala.concurrent.duration._
import akka.actor.{ Actor, ActorRef, ActorRefFactory }
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
    tested ! ChangeToGreenCommand
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", ChangingToGreenLight))
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
    expectMsg(StatusEvent("1", ChangingToRedLight))
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
    val tested = TestLight(initialState = ChangingToGreenLight)
    tested ! SetDirectorCommand(testActor)
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", ChangingToGreenLight))
    tested ! ChangeToGreenCommand //should be ignored
    tested ! ChangeToRedCommand //should change state immediately to ChangingToRedLight
    expectNoMsg(1.second)
    tested ! FinalizeChange
    tested ! GetStatusQuery
    expectMsg(ChangedToRedEvent)
    expectMsg(StatusEvent("1", RedLight))
  }

  it should "switch to green when orange light before red" in new ActorSystemTest {
    val tested = TestLight(initialState = ChangingToRedLight)
    tested ! SetDirectorCommand(testActor)
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", ChangingToRedLight))
    tested ! ChangeToRedCommand //should be ignored
    tested ! ChangeToGreenCommand //should change state immediately to ChangingToGreenLight
    expectNoMsg(1.second)
    tested ! FinalizeChange
    tested ! GetStatusQuery
    expectMsg(ChangedToGreenEvent)
    expectMsg(StatusEvent("1", GreenLight))
  }

  it should "stay green when orange before green light" in new ActorSystemTest {
    val tested = TestLight(initialState = ChangingToGreenLight)
    tested ! SetDirectorCommand(testActor)
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", ChangingToGreenLight))
    tested ! ChangeToRedCommand //should change state immediately to ChangingToRedLight
    tested ! ChangeToGreenCommand //should change state immediately back to ChangingToGreenLight
    expectNoMsg(1.second)
    tested ! FinalizeChange
    tested ! GetStatusQuery
    expectMsg(ChangedToGreenEvent)
    expectMsg(StatusEvent("1", GreenLight))
  }

  it should "stay red when orange before red light" in new ActorSystemTest {
    val tested = TestLight(initialState = ChangingToRedLight)
    tested ! SetDirectorCommand(testActor)
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", ChangingToRedLight))
    tested ! ChangeToGreenCommand //should change state immediately to ChangingToGreenLight
    tested ! ChangeToRedCommand //should change state immediately back to ChangingToRedLight
    expectNoMsg(1.second)
    tested ! FinalizeChange
    tested ! GetStatusQuery
    expectMsg(ChangedToRedEvent)
    expectMsg(StatusEvent("1", RedLight))
  }

}
