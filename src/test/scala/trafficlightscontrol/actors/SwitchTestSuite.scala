package trafficlightscontrol.actors

import org.junit.runner.RunWith
import org.scalatest.{ FlatSpecLike, Matchers }
import org.scalatest.concurrent.ScalaFutures
import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestActorRef, TestKit }
import com.typesafe.config.ConfigFactory
import akka.testkit.{ TestProbe, EventFilter }
import scala.concurrent.duration._
import akka.actor.ActorRef

trait SwitchTestSuite extends FlatSpecLike with Matchers with ScalaFutures with ActorSystemTestKit {

  def runSuite(name: String, switch: (String, Seq[LightState]) => TestActorRef[_]) {

    s"A $name" should "change status of light #1 from red to green when all are red" in new ActorSystemTest {
      val tested = switch("s-1", Seq(RedLight, RedLight, RedLight, RedLight))
      tested ! RegisterRecipientCommand(self)
      expectMsg(RecipientRegisteredEvent("s-1"))
      Thread.sleep(100)
      tested ! ChangeToGreenCommand
      expectMsg(ChangedToGreenEvent)
    }

    it should "change status of light #1 from red to green when only light #2 is green" in new ActorSystemTest {
      val tested = switch("s-1", Seq(RedLight, GreenLight, RedLight, RedLight))
      tested ! RegisterRecipientCommand(self)
      expectMsg(RecipientRegisteredEvent("s-1"))
      Thread.sleep(100)
      eventListener.expectMsgAllOf(checkTimeout,
        StatusEvent("1", RedLight),
        StatusEvent("2", GreenLight),
        StatusEvent("3", RedLight),
        StatusEvent("4", RedLight)
      )
      tested ! ChangeToGreenCommand
      expectMsg(ChangedToGreenEvent)
      eventListener.expectMsgAllOf(checkTimeout,
        StatusEvent("1", ChangingToGreenLight),
        StatusEvent("1", GreenLight),
        StatusEvent("2", ChangingToRedLight),
        StatusEvent("2", RedLight)
      )
    }

    it should "change status of light #1 from red to green when only all others are red" in new ActorSystemTest {
      val tested = switch("s-1", Seq(RedLight, GreenLight, GreenLight, GreenLight))
      tested ! RegisterRecipientCommand(self)
      expectMsg(RecipientRegisteredEvent("s-1"))
      Thread.sleep(100)
      eventListener.expectMsgAllOf(checkTimeout,
        StatusEvent("1", RedLight),
        StatusEvent("2", GreenLight),
        StatusEvent("3", GreenLight),
        StatusEvent("4", GreenLight)
      )
      tested ! ChangeToGreenCommand
      expectMsg(ChangedToGreenEvent)
      eventListener.expectMsgAllOf(checkTimeout,
        StatusEvent("1", ChangingToGreenLight),
        StatusEvent("1", GreenLight),
        StatusEvent("2", ChangingToRedLight),
        StatusEvent("2", RedLight),
        StatusEvent("3", ChangingToRedLight),
        StatusEvent("3", RedLight),
        StatusEvent("4", ChangingToRedLight),
        StatusEvent("4", RedLight)
      )
    }

    it should "change status of all lights to red" in new ActorSystemTest {
      val tested = switch("s-1", Seq(GreenLight, GreenLight, RedLight, GreenLight))
      tested ! RegisterRecipientCommand(self)
      expectMsg(RecipientRegisteredEvent("s-1"))
      Thread.sleep(100)
      eventListener.expectMsgAllOf(checkTimeout,
        StatusEvent("1", GreenLight),
        StatusEvent("2", GreenLight),
        StatusEvent("3", RedLight),
        StatusEvent("4", GreenLight)
      )
      tested ! ChangeToRedCommand
      expectMsg(ChangedToRedEvent)
      eventListener.expectMsgAllOf(checkTimeout,
        StatusEvent("1", ChangingToRedLight),
        StatusEvent("1", RedLight),
        StatusEvent("2", ChangingToRedLight),
        StatusEvent("2", RedLight),
        StatusEvent("4", ChangingToRedLight),
        StatusEvent("4", RedLight)
      )
    }

    it should "change status sequentially to green starting from light #1" in new ActorSystemTest {
      val tested = switch("s-1", Seq(RedLight, RedLight, RedLight, GreenLight))
      tested ! RegisterRecipientCommand(self)
      expectMsg(RecipientRegisteredEvent("s-1"))
      Thread.sleep(100)
      eventListener.expectMsgAllOf(checkTimeout,
        StatusEvent("1", RedLight),
        StatusEvent("2", RedLight),
        StatusEvent("3", RedLight),
        StatusEvent("4", GreenLight)
      )
      for (j <- 0 to 3; i <- 1 to 4) {
        val prev_id = s"${if ((i - 1) == 0) 4 else (i - 1)}"
        val id = s"${i}"
        tested ! ChangeToGreenCommand
        expectMsg(ChangedToGreenEvent)
        eventListener.expectMsgAllOf(checkTimeout,
          StatusEvent(prev_id, ChangingToRedLight),
          StatusEvent(prev_id, RedLight),
          StatusEvent(id, ChangingToGreenLight),
          StatusEvent(id, GreenLight)
        )
      }
    }
  }

}
