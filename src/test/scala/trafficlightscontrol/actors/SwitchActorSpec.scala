package trafficlightscontrol.actors

import org.scalatest.{FlatSpecLike, Matchers}
import org.scalatest.concurrent.ScalaFutures
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.typesafe.config.ConfigFactory
import akka.testkit.{EventFilter, TestProbe}
import scala.concurrent.duration._
import akka.actor.ActorRef

import trafficlightscontrol.model._
import trafficlightscontrol.actors._

class SwitchActorSpec extends FlatSpecLike with Matchers with ScalaFutures with ActorSystemTestKit {

  implicit val config = Configuration(
    interval = 1.second,
    delayGreenToRed = 60.milliseconds,
    delayRedToGreen = 40.milliseconds,
    sequenceDelay = 10.milliseconds,
    timeout = 1.second
  )

  "A SwitchActor" should "receive TickEvent and emit ChangeToGreenCommand when initially set to false" in new ActorSystemTest {
    val probe = TestProbe()
    val switch = system.actorOf(SwitchActor.props("s1", TestProps(probe), config, false))
    probe.expectMsgType[RegisterRecipientCommand]
    probe.reply(RecipientRegisteredEvent("probe"))
    switch ! TickEvent
    probe.expectMsg(ChangeToGreenCommand)
    probe.reply(ChangedToGreenEvent)
    probe.expectMsg(TickEvent)
  }

  it should "receive TickEvent and emit ChangeToRedCommand when initially set to true" in new ActorSystemTest {
    val probe = TestProbe()
    val switch = system.actorOf(SwitchActor.props("s1", TestProps(probe), config, true))
    probe.expectMsgType[RegisterRecipientCommand]
    probe.reply(RecipientRegisteredEvent("probe"))
    switch ! TickEvent
    probe.expectMsg(ChangeToRedCommand)
    probe.reply(ChangedToRedEvent)
    probe.expectMsg(TickEvent)
  }

  it should "receive sequence of TickEvents and emit Red/Green commands alternately" in new ActorSystemTest {
    val probe = TestProbe()
    val switch = system.actorOf(SwitchActor.props("s1", TestProps(probe), config, true))
    probe.expectMsgType[RegisterRecipientCommand]
    probe.reply(RecipientRegisteredEvent("probe"))
    info("first tick - expects ChangeToRedCommand")
    switch ! TickEvent
    probe.expectMsg(ChangeToRedCommand)
    probe.reply(ChangedToRedEvent)
    probe.expectMsg(TickEvent)
    info("second tick - expects ChangeToGreenCommand")
    switch ! TickEvent
    probe.expectMsg(ChangeToGreenCommand)
    probe.reply(ChangedToGreenEvent)
    probe.expectMsg(TickEvent)
    info("third tick - expects ChangeToRedCommand")
    switch ! TickEvent
    probe.expectMsg(ChangeToRedCommand)
    probe.reply(ChangedToRedEvent)
    probe.expectMsg(TickEvent)
    info("fourth tick - expects ChangeToGreenCommand")
    switch ! TickEvent
    probe.expectMsg(ChangeToGreenCommand)
    probe.reply(ChangedToGreenEvent)
    probe.expectMsg(TickEvent)
  }

  it should "ignore external ChangeToGreenCommand when idle" in new ActorSystemTest {
    val probe = TestProbe()
    val switch = system.actorOf(SwitchActor.props("s1", TestProps(probe), config, true))
    probe.expectMsgType[RegisterRecipientCommand]
    probe.reply(RecipientRegisteredEvent("probe"))
    switch ! ChangeToGreenCommand
    probe.expectNoMsg(1.second)
  }

  it should "ignore external ChangeToRedCommand when idle" in new ActorSystemTest {
    val probe = TestProbe()
    val switch = system.actorOf(SwitchActor.props("s1", TestProps(probe), config, true))
    probe.expectMsgType[RegisterRecipientCommand]
    probe.reply(RecipientRegisteredEvent("probe"))
    switch ! ChangeToRedCommand
    probe.expectNoMsg(1.second)
  }

  it should "ignore external ChangeToRedCommand when pending" in new ActorSystemTest {
    val probe = TestProbe()
    val switch = system.actorOf(SwitchActor.props("s1", TestProps(probe), config, true))
    probe.expectMsgType[RegisterRecipientCommand]
    probe.reply(RecipientRegisteredEvent("probe"))
    switch ! TickEvent
    probe.expectMsg(ChangeToRedCommand)
    probe.expectMsg(TickEvent)
    switch ! ChangeToRedCommand
    switch ! ChangeToGreenCommand
    probe.expectNoMsg(200.millis)
    switch ! ChangedToRedEvent
  }

  it should "ignore external ChangeToGreenCommand when pending" in new ActorSystemTest {
    val probe = TestProbe()
    val switch = system.actorOf(SwitchActor.props("s1", TestProps(probe), config, false))
    probe.expectMsgType[RegisterRecipientCommand]
    probe.reply(RecipientRegisteredEvent("probe"))
    switch ! TickEvent
    probe.expectMsg(ChangeToGreenCommand)
    probe.expectMsg(TickEvent)
    switch ! ChangeToGreenCommand
    switch ! ChangeToRedCommand
    probe.expectNoMsg(200.millis)
    switch ! ChangedToRedEvent
  }

  it should "skip defined number of ticks" in new ActorSystemTest {
    val probe = TestProbe()
    val switch = system.actorOf(SwitchActor.props("s1", TestProps(probe), config, false, 2))
    probe.expectMsgType[RegisterRecipientCommand]
    probe.reply(RecipientRegisteredEvent("probe"))
    switch ! TickEvent
    probe.expectMsg(ChangeToGreenCommand)
    probe.expectMsg(TickEvent)
    probe.reply(ChangedToGreenEvent)
    switch ! TickEvent
    probe.expectNoMsg(200.millis)
    switch ! TickEvent
    probe.expectNoMsg(200.millis)
    switch ! TickEvent
    probe.expectMsg(ChangeToRedCommand)
    probe.expectMsg(TickEvent)
    probe.reply(ChangedToRedEvent)
    switch ! TickEvent
    probe.expectNoMsg(200.millis)
    switch ! TickEvent
    probe.expectNoMsg(200.millis)
    switch ! TickEvent
    probe.expectMsg(ChangeToGreenCommand)
    probe.expectMsg(TickEvent)
    probe.reply(ChangedToGreenEvent)
    switch ! TickEvent
    probe.expectNoMsg(200.millis)
    switch ! TickEvent
    probe.expectNoMsg(200.millis)
    switch ! TickEvent
    probe.expectMsg(ChangeToRedCommand)
    probe.expectMsg(TickEvent)
    probe.reply(ChangedToRedEvent)
  }

}
