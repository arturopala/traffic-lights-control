package trafficlightscontrol.actors

import org.scalatest.{FlatSpecLike, Matchers}
import org.scalatest.concurrent.ScalaFutures
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.typesafe.config.ConfigFactory
import akka.testkit.{EventFilter, TestProbe}
import scala.concurrent.duration._
import akka.actor.ActorRef

import trafficlightscontrol.model._

trait SequenceTestSuite extends FlatSpecLike with Matchers with ScalaFutures with ActorSystemTestKit {

  def runSuite(name: String, sequence: (String, Seq[LightState]) => TestActorRef[_]) {

    s"A $name" should "change status of light #1 from red to green when all are red" in new ActorSystemTest {
      val tested = sequence("s1", Seq(RedLight, RedLight, RedLight, RedLight))
      tested ! RegisterRecipientCommand(self)
      expectMsg(RecipientRegisteredEvent("s1"))
      Thread.sleep(100)
      tested ! ChangeToGreenCommand
      expectMsg(ChangedToGreenEvent)
    }

    it should "change status of light #1 from red to green when only light #2 is green" in new ActorSystemTest {
      val tested = sequence("s1", Seq(RedLight, GreenLight, RedLight, RedLight))
      tested ! RegisterRecipientCommand(self)
      expectMsg(RecipientRegisteredEvent("s1"))
      Thread.sleep(100)
      eventListener.expectMsgAllOf(
        checkTimeout,
        StateChangedEvent("1", RedLight),
        StateChangedEvent("2", GreenLight),
        StateChangedEvent("3", RedLight),
        StateChangedEvent("4", RedLight)
      )
      tested ! ChangeToGreenCommand
      expectMsg(ChangedToGreenEvent)
      eventListener.expectMsgAllOf(
        checkTimeout,
        StateChangedEvent("s1", ChangingToGreenLight),
        StateChangedEvent("1", ChangingToGreenLight),
        StateChangedEvent("1", GreenLight),
        StateChangedEvent("2", ChangingToRedLight),
        StateChangedEvent("2", RedLight),
        StateChangedEvent("s1", GreenLight)
      )
    }

    it should "change status of light #1 from red to green when only all others are red" in new ActorSystemTest {
      val tested = sequence("s1", Seq(RedLight, GreenLight, GreenLight, GreenLight))
      tested ! RegisterRecipientCommand(self)
      expectMsg(RecipientRegisteredEvent("s1"))
      Thread.sleep(100)
      eventListener.expectMsgAllOf(
        checkTimeout,
        StateChangedEvent("1", RedLight),
        StateChangedEvent("2", GreenLight),
        StateChangedEvent("3", GreenLight),
        StateChangedEvent("4", GreenLight)
      )
      tested ! ChangeToGreenCommand
      expectMsg(ChangedToGreenEvent)
      eventListener.expectMsgAllOf(
        checkTimeout,
        StateChangedEvent("s1", ChangingToGreenLight),
        StateChangedEvent("1", ChangingToGreenLight),
        StateChangedEvent("1", GreenLight),
        StateChangedEvent("2", ChangingToRedLight),
        StateChangedEvent("2", RedLight),
        StateChangedEvent("3", ChangingToRedLight),
        StateChangedEvent("3", RedLight),
        StateChangedEvent("4", ChangingToRedLight),
        StateChangedEvent("4", RedLight),
        StateChangedEvent("s1", GreenLight)
      )
    }

    it should "change status of all lights to red" in new ActorSystemTest {
      val tested = sequence("s1", Seq(GreenLight, GreenLight, RedLight, GreenLight))
      tested ! RegisterRecipientCommand(self)
      expectMsg(RecipientRegisteredEvent("s1"))
      Thread.sleep(100)
      eventListener.expectMsgAllOf(
        checkTimeout,
        StateChangedEvent("1", GreenLight),
        StateChangedEvent("2", GreenLight),
        StateChangedEvent("3", RedLight),
        StateChangedEvent("4", GreenLight)
      )
      tested ! ChangeToRedCommand
      expectMsg(ChangedToRedEvent)
      eventListener.expectMsgAllOf(
        checkTimeout,
        StateChangedEvent("s1", ChangingToRedLight),
        StateChangedEvent("1", ChangingToRedLight),
        StateChangedEvent("1", RedLight),
        StateChangedEvent("2", ChangingToRedLight),
        StateChangedEvent("2", RedLight),
        StateChangedEvent("4", ChangingToRedLight),
        StateChangedEvent("4", RedLight),
        StateChangedEvent("s1", RedLight)
      )
    }

    it should "change status sequentially to green starting from light #1" in new ActorSystemTest {
      val tested = sequence("s1", Seq(RedLight, RedLight, RedLight, GreenLight))
      tested ! RegisterRecipientCommand(self)
      expectMsg(RecipientRegisteredEvent("s1"))
      Thread.sleep(100)
      eventListener.expectMsgAllOf(
        checkTimeout,
        StateChangedEvent("1", RedLight),
        StateChangedEvent("2", RedLight),
        StateChangedEvent("3", RedLight),
        StateChangedEvent("4", GreenLight)
      )
      for (j <- 0 to 3; i <- 1 to 4) {
        val prev_id = s"${if ((i - 1) == 0) 4 else (i - 1)}"
        val id = s"$i"
        tested ! ChangeToGreenCommand
        expectMsg(ChangedToGreenEvent)
        eventListener.expectMsgAllOf(
          checkTimeout,
          StateChangedEvent("s1", ChangingToGreenLight),
          StateChangedEvent(prev_id, ChangingToRedLight),
          StateChangedEvent(prev_id, RedLight),
          StateChangedEvent(id, ChangingToGreenLight),
          StateChangedEvent(id, GreenLight),
          StateChangedEvent("s1", GreenLight)
        )
      }
    }
  }

}
