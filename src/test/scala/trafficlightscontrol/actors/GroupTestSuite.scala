package trafficlightscontrol.actors

import org.scalatest.{ FlatSpecLike, Matchers }
import org.scalatest.concurrent.ScalaFutures
import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestActorRef, TestKit }
import com.typesafe.config.ConfigFactory
import akka.testkit.{ TestProbe, EventFilter }
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
        StatusEvent("1", RedLight),
        StatusEvent("2", RedLight),
        StatusEvent("3", RedLight),
        StatusEvent("4", RedLight)
      )
      tested ! ChangeToGreenCommand
      expectMsg(ChangedToGreenEvent)
      eventListener.expectMsgAllOf(
        checkTimeout,
        StatusEvent("g1", ChangingToGreenLight),
        StatusEvent("1", ChangingToGreenLight),
        StatusEvent("2", ChangingToGreenLight),
        StatusEvent("3", ChangingToGreenLight),
        StatusEvent("4", ChangingToGreenLight),
        StatusEvent("1", GreenLight),
        StatusEvent("2", GreenLight),
        StatusEvent("3", GreenLight),
        StatusEvent("4", GreenLight),
        StatusEvent("g1", GreenLight)
      )
    }

    it should "change status of lights from green to red when all are green" in new ActorSystemTest {
      val tested = group("g1", Seq(GreenLight, GreenLight, GreenLight, GreenLight))
      tested ! RegisterRecipientCommand(self)
      expectMsg(RecipientRegisteredEvent("g1"))
      Thread.sleep(100)
      eventListener.expectMsgAllOf(
        checkTimeout,
        StatusEvent("1", GreenLight),
        StatusEvent("2", GreenLight),
        StatusEvent("3", GreenLight),
        StatusEvent("4", GreenLight)
      )
      tested ! ChangeToRedCommand
      expectMsg(ChangedToRedEvent)
      eventListener.expectMsgAllOf(
        checkTimeout,
        StatusEvent("g1", ChangingToRedLight),
        StatusEvent("1", ChangingToRedLight),
        StatusEvent("2", ChangingToRedLight),
        StatusEvent("3", ChangingToRedLight),
        StatusEvent("4", ChangingToRedLight),
        StatusEvent("1", RedLight),
        StatusEvent("2", RedLight),
        StatusEvent("3", RedLight),
        StatusEvent("4", RedLight),
        StatusEvent("g1", RedLight)
      )
    }

    s"A $name" should "change status of lights from red to green when some are red" in new ActorSystemTest {
      val tested = group("g1", Seq(RedLight, GreenLight, RedLight, RedLight))
      tested ! RegisterRecipientCommand(self)
      expectMsg(RecipientRegisteredEvent("g1"))
      Thread.sleep(100)
      eventListener.expectMsgAllOf(
        checkTimeout,
        StatusEvent("1", RedLight),
        StatusEvent("2", GreenLight),
        StatusEvent("3", RedLight),
        StatusEvent("4", RedLight)
      )
      tested ! ChangeToGreenCommand
      expectMsg(ChangedToGreenEvent)
      eventListener.expectMsgAllOf(
        checkTimeout,
        StatusEvent("g1", ChangingToGreenLight),
        StatusEvent("1", ChangingToGreenLight),
        StatusEvent("3", ChangingToGreenLight),
        StatusEvent("4", ChangingToGreenLight),
        StatusEvent("1", GreenLight),
        StatusEvent("3", GreenLight),
        StatusEvent("4", GreenLight),
        StatusEvent("g1", GreenLight)
      )
    }

    it should "change status of lights from green to red when some are green" in new ActorSystemTest {
      val tested = group("g1", Seq(GreenLight, GreenLight, RedLight, GreenLight))
      tested ! RegisterRecipientCommand(self)
      expectMsg(RecipientRegisteredEvent("g1"))
      Thread.sleep(100)
      eventListener.expectMsgAllOf(
        checkTimeout,
        StatusEvent("1", GreenLight),
        StatusEvent("2", GreenLight),
        StatusEvent("3", RedLight),
        StatusEvent("4", GreenLight)
      )
      tested ! ChangeToRedCommand
      expectMsg(ChangedToRedEvent)
      eventListener.expectMsgAllOf(
        checkTimeout,
        StatusEvent("g1", ChangingToRedLight),
        StatusEvent("1", ChangingToRedLight),
        StatusEvent("2", ChangingToRedLight),
        StatusEvent("4", ChangingToRedLight),
        StatusEvent("1", RedLight),
        StatusEvent("2", RedLight),
        StatusEvent("4", RedLight),
        StatusEvent("g1", RedLight)
      )
    }

  }

}
