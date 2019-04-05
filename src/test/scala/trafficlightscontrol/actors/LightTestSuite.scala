package trafficlightscontrol.actors

import org.scalatest.{FlatSpecLike, Matchers}
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.typesafe.config.ConfigFactory
import akka.testkit.TestProbe
import scala.concurrent.duration._
import akka.actor.{Actor, ActorRef, ActorRefFactory}

import trafficlightscontrol.model._

trait LightTestSuite extends FlatSpecLike with Matchers with ActorSystemTestKit {

  object Ack extends Event

  def runSuite(name: String, light: (String, LightState) => TestActorRef[_]) = {

    s"A $name" should "change status from red to green" in new ActorSystemTest {
      val tested = light("1", RedLight)
      info("register recipient")
      tested ! RegisterRecipientCommand(self)
      expectMsg(RecipientRegisteredEvent("1"))
      info("get status of light #1")
      tested ! GetStatusQuery
      expectMsg(StateChangedEvent("1", RedLight))
      info("change light #1 to green")
      tested ! ChangeToGreenCommand
      tested ! GetStatusQuery
      expectMsg(StateChangedEvent("1", ChangingToGreenLight))
      expectMsg(checkTimeout, ChangedToGreenEvent)
      info("assert status of light #1")
      tested ! GetStatusQuery
      expectMsg(StateChangedEvent("1", GreenLight))
    }

    it should "change status from green to red" in new ActorSystemTest {
      val tested = light("1", GreenLight)
      tested ! RegisterRecipientCommand(testActor)
      expectMsg(RecipientRegisteredEvent("1"))
      tested ! GetStatusQuery
      expectMsg(StateChangedEvent("1", GreenLight))
      tested ! ChangeToRedCommand
      tested ! GetStatusQuery
      expectMsg(StateChangedEvent("1", ChangingToRedLight))
      expectMsg(checkTimeout, ChangedToRedEvent)
      tested ! GetStatusQuery
      expectMsg(StateChangedEvent("1", RedLight))
    }

    it should "return current status" in new ActorSystemTest {
      val greenLight = light("A", GreenLight)
      greenLight ! RegisterRecipientCommand(testActor)
      expectMsg(RecipientRegisteredEvent("A"))
      val redLight = light("B", RedLight)
      redLight ! RegisterRecipientCommand(testActor)
      expectMsg(RecipientRegisteredEvent("B"))
      greenLight ! GetStatusQuery
      expectMsg(StateChangedEvent("A", GreenLight))
      redLight ! GetStatusQuery
      expectMsg(StateChangedEvent("B", RedLight))
    }

    it should "sequence to red when yellow light before green" in new ActorSystemTest {
      val tested = light("1", ChangingToGreenLight)
      tested ! RegisterRecipientCommand(testActor)
      expectMsg(RecipientRegisteredEvent("1"))
      tested ! GetStatusQuery
      expectMsg(StateChangedEvent("1", ChangingToGreenLight))
      tested ! ChangeToGreenCommand //should be ignored
      tested ! ChangeToRedCommand //should change state immediately to ChangingToRedLight
      expectNoMsg(1.second)
      tested ! CanContinueAfterDelayEvent
      tested ! GetStatusQuery
      expectMsg(ChangedToRedEvent)
      expectMsg(StateChangedEvent("1", RedLight))
    }

    it should "sequence to green when yellow light before red" in new ActorSystemTest {
      val tested = light("1", ChangingToRedLight)
      tested ! RegisterRecipientCommand(testActor)
      expectMsg(RecipientRegisteredEvent("1"))
      tested ! GetStatusQuery
      expectMsg(StateChangedEvent("1", ChangingToRedLight))
      tested ! ChangeToRedCommand //should be ignored
      tested ! ChangeToGreenCommand //should change state immediately to ChangingToGreenLight
      expectNoMsg(1.second)
      tested ! CanContinueAfterDelayEvent
      tested ! GetStatusQuery
      expectMsg(ChangedToGreenEvent)
      expectMsg(StateChangedEvent("1", GreenLight))
    }

    it should "stay green when yellow before green light" in new ActorSystemTest {
      val tested = light("1", ChangingToGreenLight)
      tested ! RegisterRecipientCommand(testActor)
      expectMsg(RecipientRegisteredEvent("1"))
      tested ! GetStatusQuery
      expectMsg(StateChangedEvent("1", ChangingToGreenLight))
      tested ! ChangeToRedCommand //should change state immediately to ChangingToRedLight
      tested ! ChangeToGreenCommand //should change state immediately back to ChangingToGreenLight
      expectNoMsg(1.second)
      tested ! CanContinueAfterDelayEvent
      tested ! GetStatusQuery
      expectMsg(ChangedToGreenEvent)
      expectMsg(StateChangedEvent("1", GreenLight))
    }

    it should "stay red when yellow before red light" in new ActorSystemTest {
      val tested = light("1", ChangingToRedLight)
      tested ! RegisterRecipientCommand(testActor)
      expectMsg(RecipientRegisteredEvent("1"))
      tested ! GetStatusQuery
      expectMsg(StateChangedEvent("1", ChangingToRedLight))
      tested ! ChangeToGreenCommand //should change state immediately to ChangingToGreenLight
      tested ! ChangeToRedCommand //should change state immediately back to ChangingToRedLight
      expectNoMsg(1.second)
      tested ! CanContinueAfterDelayEvent
      tested ! GetStatusQuery
      expectMsg(ChangedToRedEvent)
      expectMsg(StateChangedEvent("1", RedLight))
    }
  }

}
