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
import trafficlightscontrol.actors._

class PulseActorSpec extends FlatSpecLike with Matchers with ScalaFutures with ActorSystemTestKit {

  implicit val config = Configuration(
    interval = 1.second,
    delayGreenToRed = 60.milliseconds,
    delayRedToGreen = 40.milliseconds,
    sequenceDelay = 10.milliseconds,
    timeout = 1.second
  )

  object TestCommand extends Command
  object TestEvent extends Event

  "A PulseActor" should "receive TickEvents and emit default ChangeToGreenCommand, then wait for a default ChangedToGreenEvent" in new ActorSystemTest {

    val probe = TestProbe()
    val pulse = system.actorOf(PulseActor.props("s1", TestProps(probe), config))
    probe.expectMsgType[RegisterRecipientCommand]
    probe.reply(RecipientRegisteredEvent("probe"))
    (1 to 10) foreach { i =>
      pulse ! TickEvent //first tick event when pulse is idle
      probe.expectMsg(ChangeToGreenCommand)
      probe.expectMsg(TickEvent)
      pulse ! ChangeToGreenCommand //external command should be ignored
      pulse ! TickEvent //second tick event when pulse is pending
      probe.reply(ChangedToGreenEvent)
      probe.expectMsg(TickEvent)
    }
  }

  it should "receive TickEvents and emit given command, then wait for a given ackEvent" in new ActorSystemTest {

    val probe = TestProbe()
    val pulse = system.actorOf(PulseActor.props("s1", TestProps(probe), config, TestCommand, TestEvent))
    probe.expectMsgType[RegisterRecipientCommand]
    probe.reply(RecipientRegisteredEvent("probe"))
    (1 to 10) foreach { i =>
      pulse ! TickEvent //first tick event when pulse is idle
      probe.expectMsg(TestCommand)
      probe.expectMsg(TickEvent)
      pulse ! TestCommand //send command from outside
      pulse ! TickEvent //second tick event when pulse is pending
      probe.reply(TestEvent)
      probe.expectMsg(TickEvent)
      //expectMsg(MessageIgnoredEvent(TestCommand)) //external command should be ignored
    }
  }

}
