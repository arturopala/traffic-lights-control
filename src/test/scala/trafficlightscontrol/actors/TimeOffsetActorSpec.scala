package trafficlightscontrol.actors

import org.scalatest.{ FlatSpecLike, Matchers }
import org.scalatest.concurrent.ScalaFutures
import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestActorRef, TestKit }
import com.typesafe.config.ConfigFactory
import akka.testkit.{ TestProbe, EventFilter }
import scala.concurrent.duration._
import akka.actor.ActorRef

import trafficlightscontrol.model._

import org.scalatest.prop.PropertyChecks
import org.scalatest.junit.JUnitRunner
import org.scalacheck._

class TimeOffsetActorSpec extends FlatSpecLike with Matchers with ScalaFutures with PropertyChecks with ActorSystemTestKit {

  implicit val config = Configuration(
    interval = 1.second,
    delayGreenToRed = 60.milliseconds,
    delayRedToGreen = 40.milliseconds,
    sequenceDelay = 10.milliseconds,
    timeout = 1.second
  )

  "A TimeOffsetActor" should "delay message passing to the member by an offset time" in new ActorSystemTest {

    val probe = TestProbe()
    val pulse = system.actorOf(TimeOffsetActor.props("p1", TestProps(probe), config, 500.millis))
    probe.expectMsgType[RegisterRecipientCommand]
    probe.reply(RecipientRegisteredEvent("probe"))
    (1 to 5) foreach { i =>
      pulse ! TickEvent
      probe.expectNoMsg(450.millis)
      probe.expectMsg(100.millis, TickEvent)
    }
    (1 to 5) foreach { i =>
      pulse ! ChangeToGreenCommand
      probe.expectNoMsg(450.millis)
      probe.expectMsg(100.millis, ChangeToGreenCommand)
    }
  }

}
