package trafficlightscontrol

import org.junit.runner.RunWith
import org.scalatest.{ FlatSpecLike, Matchers }
import org.scalatest.concurrent.ScalaFutures
import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestActorRef, TestKit }
import com.typesafe.config.ConfigFactory
import akka.testkit.{ TestProbe, EventFilter }
import scala.concurrent.duration._
import akka.actor.ActorRef
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SwitchSpec extends FlatSpecLike with Matchers with ScalaFutures with ActorSystemTestKit with TrafficSystemTestKit {

  "A Switch" should "change status of light #1 from red to green" in new ActorSystemTest {
    val tested = TestSwitch(Seq(RedLight, RedLight, RedLight, RedLight))
    tested ! RegisterDirectorCommand(self)
    expectMsg(DirectorRegisteredEvent(""))
    tested ! ChangeToGreenCommand
    expectMsg(ChangedToGreenEvent)
  }

  it should "change status of light #1 from red to green when only light #2 is green" in new ActorSystemTest {
    val tested = TestSwitch(Seq(RedLight, GreenLight, RedLight, RedLight))
    tested ! RegisterDirectorCommand(self)
    expectMsg(DirectorRegisteredEvent(""))
    eventListener.expectMsgAllOf(
      StatusEvent("1", RedLight),
      StatusEvent("2", GreenLight),
      StatusEvent("3", RedLight),
      StatusEvent("4", RedLight)
    )
    tested ! ChangeToGreenCommand
    expectMsg(ChangedToGreenEvent)
    eventListener.expectMsgAllOf(
      StatusEvent("1", ChangingToGreenLight),
      StatusEvent("1", GreenLight),
      StatusEvent("2", ChangingToRedLight),
      StatusEvent("2", RedLight)
    )
  }

  it should "change status of light #1 from red to green when only all others are red" in new ActorSystemTest {
    val tested = TestSwitch(Seq(RedLight, GreenLight, GreenLight, GreenLight))
    tested ! RegisterDirectorCommand(self)
    expectMsg(DirectorRegisteredEvent(""))
    eventListener.expectMsgAllOf(
      StatusEvent("1", RedLight),
      StatusEvent("2", GreenLight),
      StatusEvent("3", GreenLight),
      StatusEvent("4", GreenLight)
    )
    tested ! ChangeToGreenCommand
    expectMsg(ChangedToGreenEvent)
    eventListener.expectMsgAllOf(
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
    val tested = TestSwitch(Seq(GreenLight, GreenLight, RedLight, GreenLight))
    tested ! RegisterDirectorCommand(self)
    expectMsg(DirectorRegisteredEvent(""))
    eventListener.expectMsgAllOf(
      StatusEvent("1", GreenLight),
      StatusEvent("2", GreenLight),
      StatusEvent("3", RedLight),
      StatusEvent("4", GreenLight)
    )
    tested ! ChangeToRedCommand
    expectMsg(ChangedToRedEvent)
    eventListener.expectMsgAllOf(
      StatusEvent("1", ChangingToRedLight),
      StatusEvent("1", RedLight),
      StatusEvent("2", ChangingToRedLight),
      StatusEvent("2", RedLight),
      StatusEvent("4", ChangingToRedLight),
      StatusEvent("4", RedLight)
    )
  }

}
