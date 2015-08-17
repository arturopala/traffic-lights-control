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
class LightFSMSpec extends FlatSpecLike with Matchers with ActorSystemTestKit with TrafficSystemTestKit {

  "A LightFSM" should "change status from red to green" in new ActorSystemTest {
    val tested = TestLightFSM(initialState = RedLight)
    tested ! RegisterRecipientCommand(testActor)
    expectMsg(RecipientRegisteredEvent("1"))
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
    val tested = TestLightFSM(initialState = GreenLight)
    tested ! RegisterRecipientCommand(testActor)
    expectMsg(RecipientRegisteredEvent("1"))
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", GreenLight))
    tested ! ChangeToRedCommand
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", ChangingToRedLight))
    expectMsg(testLightChangeDelay * 2, ChangedToRedEvent)
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", RedLight))
  }

  it should "reply current state event" in new ActorSystemTest {
    val greenLight = TestLightFSM(id = "A", initialState = GreenLight)
    greenLight ! RegisterRecipientCommand(testActor)
    expectMsg(RecipientRegisteredEvent("A"))
    val redLight = TestLightFSM(id = "B", initialState = RedLight)
    redLight ! RegisterRecipientCommand(testActor)
    expectMsg(RecipientRegisteredEvent("B"))
    greenLight ! GetStatusQuery
    expectMsg(StatusEvent("A", GreenLight))
    redLight ! GetStatusQuery
    expectMsg(StatusEvent("B", RedLight))
  }

  it should "switch to red when orange light before green" in new ActorSystemTest {
    val tested = TestLightFSM(initialState = ChangingToGreenLight)
    tested ! RegisterRecipientCommand(testActor)
    expectMsg(RecipientRegisteredEvent("1"))
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", ChangingToGreenLight))
    tested ! ChangeToGreenCommand
    tested ! ChangeToRedCommand
    expectNoMsg(1.second)
    tested ! FinalizeChange
    tested ! GetStatusQuery
    expectMsg(ChangedToRedEvent)
    expectMsg(StatusEvent("1", RedLight))
  }

  it should "switch to green when orange light before red" in new ActorSystemTest {
    val tested = TestLightFSM(initialState = ChangingToRedLight)
    tested ! RegisterRecipientCommand(testActor)
    expectMsg(RecipientRegisteredEvent("1"))
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
    val tested = TestLightFSM(initialState = ChangingToGreenLight)
    tested ! RegisterRecipientCommand(testActor)
    expectMsg(RecipientRegisteredEvent("1"))
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
    val tested = TestLightFSM(initialState = ChangingToRedLight)
    tested ! RegisterRecipientCommand(testActor)
    expectMsg(RecipientRegisteredEvent("1"))
    tested ! GetStatusQuery
    expectMsg(StatusEvent("1", ChangingToRedLight))
    tested ! ChangeToGreenCommand
    tested ! ChangeToRedCommand
    expectNoMsg(1.second)
    tested ! FinalizeChange
    tested ! GetStatusQuery
    expectMsg(ChangedToRedEvent)
    expectMsg(StatusEvent("1", RedLight))
  }

}
