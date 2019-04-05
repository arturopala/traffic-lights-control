package trafficlightscontrol.actors

import org.scalatest.{FlatSpecLike, Matchers}
import org.scalatest.concurrent.ScalaFutures
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.typesafe.config.ConfigFactory
import akka.testkit.{EventFilter, TestProbe}
import scala.concurrent.duration._
import akka.actor.ActorRef
import org.scalatest.prop.PropertyChecks
import org.scalacheck._

import trafficlightscontrol.model._

trait GroupTestSuite extends FlatSpecLike with Matchers with ScalaFutures with PropertyChecks with ActorSystemTestKit {

  def runSuite(name: String, group: (String, Seq[LightState]) => TestActorRef[_]) {

    s"A $name" should "change status of lights from red to green when all are red" in new ActorSystemTest {
      val tested = group("g1", Seq(RedLight, RedLight, RedLight, RedLight))
      tested ! RegisterRecipientCommand(self)
      expectMsg(RecipientRegisteredEvent("g1"))
      Thread.sleep(100)
      eventListener.expectMsgAllOf(
        checkTimeout,
        StateChangedEvent("1", RedLight),
        StateChangedEvent("2", RedLight),
        StateChangedEvent("3", RedLight),
        StateChangedEvent("4", RedLight)
      )
      tested ! ChangeToGreenCommand
      expectMsg(ChangedToGreenEvent)
      eventListener.expectMsgAllOf(
        checkTimeout,
        StateChangedEvent("g1", ChangingToGreenLight),
        StateChangedEvent("1", ChangingToGreenLight),
        StateChangedEvent("2", ChangingToGreenLight),
        StateChangedEvent("3", ChangingToGreenLight),
        StateChangedEvent("4", ChangingToGreenLight),
        StateChangedEvent("1", GreenLight),
        StateChangedEvent("2", GreenLight),
        StateChangedEvent("3", GreenLight),
        StateChangedEvent("4", GreenLight),
        StateChangedEvent("g1", GreenLight)
      )
    }

    it should "change status of lights from green to red when all are green" in new ActorSystemTest {
      val tested = group("g1", Seq(GreenLight, GreenLight, GreenLight, GreenLight))
      tested ! RegisterRecipientCommand(self)
      expectMsg(RecipientRegisteredEvent("g1"))
      Thread.sleep(100)
      eventListener.expectMsgAllOf(
        checkTimeout,
        StateChangedEvent("1", GreenLight),
        StateChangedEvent("2", GreenLight),
        StateChangedEvent("3", GreenLight),
        StateChangedEvent("4", GreenLight)
      )
      tested ! ChangeToRedCommand
      expectMsg(ChangedToRedEvent)
      eventListener.expectMsgAllOf(
        checkTimeout,
        StateChangedEvent("g1", ChangingToRedLight),
        StateChangedEvent("1", ChangingToRedLight),
        StateChangedEvent("2", ChangingToRedLight),
        StateChangedEvent("3", ChangingToRedLight),
        StateChangedEvent("4", ChangingToRedLight),
        StateChangedEvent("1", RedLight),
        StateChangedEvent("2", RedLight),
        StateChangedEvent("3", RedLight),
        StateChangedEvent("4", RedLight),
        StateChangedEvent("g1", RedLight)
      )
    }

    s"A $name" should "change status of lights from red to green when some are red" in new ActorSystemTest {
      val tested = group("g1", Seq(RedLight, GreenLight, RedLight, RedLight))
      tested ! RegisterRecipientCommand(self)
      expectMsg(RecipientRegisteredEvent("g1"))
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
        StateChangedEvent("g1", ChangingToGreenLight),
        StateChangedEvent("1", ChangingToGreenLight),
        StateChangedEvent("3", ChangingToGreenLight),
        StateChangedEvent("4", ChangingToGreenLight),
        StateChangedEvent("1", GreenLight),
        StateChangedEvent("3", GreenLight),
        StateChangedEvent("4", GreenLight),
        StateChangedEvent("g1", GreenLight)
      )
    }

    it should "change status of lights from green to red when some are green" in new ActorSystemTest {
      val tested = group("g1", Seq(GreenLight, GreenLight, RedLight, GreenLight))
      tested ! RegisterRecipientCommand(self)
      expectMsg(RecipientRegisteredEvent("g1"))
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
        StateChangedEvent("g1", ChangingToRedLight),
        StateChangedEvent("1", ChangingToRedLight),
        StateChangedEvent("2", ChangingToRedLight),
        StateChangedEvent("4", ChangingToRedLight),
        StateChangedEvent("1", RedLight),
        StateChangedEvent("2", RedLight),
        StateChangedEvent("4", RedLight),
        StateChangedEvent("g1", RedLight)
      )
    }

  }

}
