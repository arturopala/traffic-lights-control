package trafficlightscontrol.actors

import org.scalatest.{ FlatSpecLike, Matchers }
import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestActorRef, TestKit }
import com.typesafe.config.ConfigFactory
import akka.testkit.TestProbe
import scala.concurrent.duration._
import akka.actor.{ Actor, ActorRef, ActorRefFactory }

import trafficlightscontrol.model._

trait LightTestSuite extends FlatSpecLike with Matchers with ActorSystemTestKit {

  object Ack extends Event

  def runSuite(name: String, light: (String, LightState) => TestActorRef[_]) = {

    s"A $name" should "change status from red to green" in new ActorSystemTest {
      val tested = light("1", RedLight)
      tested ! RegisterRecipientCommand(self)
      expectMsg(RecipientRegisteredEvent("1"))
      tested ! GetStatusQuery
      expectMsg(StatusEvent("1", RedLight))
      tested ! ChangeToGreenCommand
      tested ! GetStatusQuery
      expectMsg(StatusEvent("1", ChangingToGreenLight))
      expectMsg(checkTimeout, ChangedToGreenEvent)
      tested ! GetStatusQuery
      expectMsg(StatusEvent("1", GreenLight))
    }

    it should "change status from green to red" in new ActorSystemTest {
      val tested = light("1", GreenLight)
      tested ! RegisterRecipientCommand(testActor)
      expectMsg(RecipientRegisteredEvent("1"))
      tested ! GetStatusQuery
      expectMsg(StatusEvent("1", GreenLight))
      tested ! ChangeToRedCommand
      tested ! GetStatusQuery
      expectMsg(StatusEvent("1", ChangingToRedLight))
      expectMsg(checkTimeout, ChangedToRedEvent)
      tested ! GetStatusQuery
      expectMsg(StatusEvent("1", RedLight))
    }

    it should "return current status" in new ActorSystemTest {
      val greenLight = light("A", GreenLight)
      greenLight ! RegisterRecipientCommand(testActor)
      expectMsg(RecipientRegisteredEvent("A"))
      val redLight = light("B", RedLight)
      redLight ! RegisterRecipientCommand(testActor)
      expectMsg(RecipientRegisteredEvent("B"))
      greenLight ! GetStatusQuery
      expectMsg(StatusEvent("A", GreenLight))
      redLight ! GetStatusQuery
      expectMsg(StatusEvent("B", RedLight))
    }

    it should "switch to red when yellow light before green" in new ActorSystemTest {
      val tested = light("1", ChangingToGreenLight)
      tested ! RegisterRecipientCommand(testActor)
      expectMsg(RecipientRegisteredEvent("1"))
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

    it should "switch to green when yellow light before red" in new ActorSystemTest {
      val tested = light("1", ChangingToRedLight)
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

    it should "stay green when yellow before green light" in new ActorSystemTest {
      val tested = light("1", ChangingToGreenLight)
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

    it should "stay red when yellow before red light" in new ActorSystemTest {
      val tested = light("1", ChangingToRedLight)
      tested ! RegisterRecipientCommand(testActor)
      expectMsg(RecipientRegisteredEvent("1"))
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

}
