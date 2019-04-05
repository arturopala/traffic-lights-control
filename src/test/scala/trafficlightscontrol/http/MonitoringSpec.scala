package trafficlightscontrol.http

import java.nio.charset.Charset
import java.nio.file.{FileSystems, Files, Path}
import java.util.function.Consumer

import org.scalatest.{Finders, FlatSpecLike, Matchers}
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.scalatest.junit.JUnitRunner
import com.typesafe.config.ConfigFactory
import akka.testkit.TestProbe
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import java.util.concurrent.atomic.AtomicInteger
import akka.actor.ActorRef

import trafficlightscontrol.actors._
import trafficlightscontrol.model._

import org.reactivestreams.{Publisher, Subscriber, Subscription}

class MonitoringSpec extends FlatSpecLike with Matchers with ActorSystemTestKit {

  class MonitoringTest extends ActorSystemTest {
    val trafficSystem = TestProbe()
    val tested = TestActorRef(new MonitoringActor())
  }

  "A Monitoring actor" must "receive StateChangedEvent and register current state of light" in new MonitoringTest {
    tested ! StateChangedEvent("1", RedLight)
    tested.underlyingActor.report("1") should equal(RedLight)
    tested ! StateChangedEvent("1", GreenLight)
    tested.underlyingActor.report("1") should equal(GreenLight)
    tested ! StateChangedEvent("2", RedLight)
    tested.underlyingActor.report("1") should equal(GreenLight)
    tested.underlyingActor.report("2") should equal(RedLight)
    tested ! StateChangedEvent("2", GreenLight)
    tested.underlyingActor.report("1") should equal(GreenLight)
    tested.underlyingActor.report("2") should equal(GreenLight)
    tested ! StateChangedEvent("1", RedLight)
    tested.underlyingActor.report("1") should equal(RedLight)
    tested.underlyingActor.report("2") should equal(GreenLight)
  }

  it must "receive GetReportQuery and send back current system status report as a ReportEvent" in new MonitoringTest {
    info("case 1: when none event yet happened")
    tested ! GetReportQuery
    val r1 = expectMsgAnyClassOf(classOf[ReportEvent])
    r1.report should have size 0

    info("case 2: when light #1 became red")
    tested ! StateChangedEvent("1", RedLight)
    tested ! GetReportQuery
    val r2 = expectMsgAnyClassOf(classOf[ReportEvent])
    r2 should not be r1
    r2.report should have size 1
    r2.report.get("1") should be(Some(RedLight))

    info("case 3: when light #1 became green")
    tested ! StateChangedEvent("1", GreenLight)
    tested ! GetReportQuery
    val r3 = expectMsgAnyClassOf(classOf[ReportEvent])
    r3.report should have size 1
    r3.report.get("1") should be(Some(GreenLight))

    info("case 4: when light #2 became red")
    tested ! StateChangedEvent("2", RedLight)
    tested ! GetReportQuery
    val r4 = expectMsgAnyClassOf(classOf[ReportEvent])
    r4.report should have size 2
    r4.report.get("1") should be(Some(GreenLight))
    r4.report.get("2") should be(Some(RedLight))

    info("case 5: when light #2 became green")
    tested ! StateChangedEvent("2", GreenLight)
    tested ! GetReportQuery
    val r5 = expectMsgAnyClassOf(classOf[ReportEvent])
    r5.report should have size 2
    r5.report.get("1") should be(Some(GreenLight))
    r5.report.get("2") should be(Some(GreenLight))

    info("case 6: when light #1 became again red")
    tested ! StateChangedEvent("1", RedLight)
    tested ! GetReportQuery
    val r6 = expectMsgAnyClassOf(classOf[ReportEvent])
    r6.report should have size 2
    r6.report.get("1") should be(Some(RedLight))
    r6.report.get("2") should be(Some(GreenLight))

    info("case 7: when asking again without any new event")
    tested ! GetReportQuery
    val r7 = expectMsgAnyClassOf(classOf[ReportEvent])
    r7.report should have size 2
    r7.report.get("1") should be(Some(RedLight))
    r7.report.get("2") should be(Some(GreenLight))
  }

  it must "receive GetReportQuery(demo) and send back current system status report as a ReportEvent" in new MonitoringTest {
    info("case 1: when none event yet happened")
    tested ! GetReportQuery("demo")
    val r1 = expectMsgAnyClassOf(classOf[ReportEvent])
    r1.report should have size 0

    info("case 2: when light #demo_1 became red")
    tested ! StateChangedEvent("demo_1", RedLight)
    tested ! StateChangedEvent("foo_1", GreenLight)
    tested ! GetReportQuery("demo")
    val r2 = expectMsgAnyClassOf(classOf[ReportEvent])
    r2 should not be r1
    r2.report should have size 1
    r2.report.get("demo_1") should be(Some(RedLight))
    r2.report.get("foo_1") shouldBe None

    info("case 3: when light #demo_1 became green")
    tested ! StateChangedEvent("demo_1", GreenLight)
    tested ! StateChangedEvent("foo_1", RedLight)
    tested ! GetReportQuery("demo")
    val r3 = expectMsgAnyClassOf(classOf[ReportEvent])
    r3.report should have size 1
    r3.report.get("demo_1") should be(Some(GreenLight))
    r3.report.get("foo_1") shouldBe None

    info("case 4: when light #demo_2 became red")
    tested ! StateChangedEvent("demo_2", RedLight)
    tested ! StateChangedEvent("foo_2", GreenLight)
    tested ! GetReportQuery("demo")
    val r4 = expectMsgAnyClassOf(classOf[ReportEvent])
    r4.report should have size 2
    r4.report.get("demo_1") should be(Some(GreenLight))
    r4.report.get("demo_2") should be(Some(RedLight))

    info("case 5: when light #demo_2 became green")
    tested ! StateChangedEvent("demo_2", GreenLight)
    tested ! GetReportQuery("demo")
    val r5 = expectMsgAnyClassOf(classOf[ReportEvent])
    r5.report should have size 2
    r5.report.get("demo_1") should be(Some(GreenLight))
    r5.report.get("demo_2") should be(Some(GreenLight))

    info("case 6: when light #demo_1 became again red")
    tested ! StateChangedEvent("demo_1", RedLight)
    tested ! GetReportQuery("demo")
    val r6 = expectMsgAnyClassOf(classOf[ReportEvent])
    r6.report should have size 2
    r6.report.get("demo_1") should be(Some(RedLight))
    r6.report.get("demo_2") should be(Some(GreenLight))
    r6.report.get("foo_1") shouldBe None
    r6.report.get("foo_2") shouldBe None

    info("case 7: when asking again without any new event")
    tested ! GetReportQuery("demo")
    val r7 = expectMsgAnyClassOf(classOf[ReportEvent])
    r7.report should have size 2
    r7.report.get("demo_1") should be(Some(RedLight))
    r7.report.get("demo_2") should be(Some(GreenLight))
  }

  it must "receive GetStatusQuery(id=1) and send back light status as an Option[StateChangedEvent]" in new MonitoringTest {
    info("case 1: when light #1 state is not known yet")
    tested ! GetStatusQuery("1")
    val r1 = expectMsgAnyClassOf(classOf[Option[StateChangedEvent]])
    r1 should be(None)

    info("case 2: when light #2 state but not #1 has been reported")
    tested ! StateChangedEvent("2", GreenLight)
    tested ! GetStatusQuery("1")
    val r2 = expectMsgAnyClassOf(classOf[Option[StateChangedEvent]])
    r2 should be(None)

    info("case 3: when light #1 state is already known")
    tested ! StateChangedEvent("1", GreenLight)
    tested ! GetStatusQuery("1")
    val r3 = expectMsgAnyClassOf(classOf[Option[StateChangedEvent]])
    r3 should be(Some(StateChangedEvent("1", GreenLight)))
  }

  it must "receive GetPublisherQuery and send back Publisher[StateChangedEvent]" in new MonitoringTest {
    info("step 1: request publisher")
    tested ! GetPublisherQuery(_ => true)
    val publisher = expectMsgAnyClassOf(classOf[Publisher[StateChangedEvent]])
    info("step 2: subscribe for events")
    val subscriber = new TestSubscriber[StateChangedEvent]
    publisher.subscribe(subscriber)
    val subscription = subscriber.probe.expectMsgType[Subscription]
    subscription.request(5)
    info("step 3: request 5 events and expect StateChangedEvent for light #1")
    tested ! StateChangedEvent("1", GreenLight)
    subscriber.probe.expectMsgType[StateChangedEvent] should be(StateChangedEvent("1", GreenLight))
    info("step 4: expect StateChangedEvent for light #2")
    tested ! StateChangedEvent("2", RedLight)
    subscriber.probe.expectMsgType[StateChangedEvent] should be(StateChangedEvent("2", RedLight))
    info("step 5: expect 3 more StateChangedEvents")
    tested ! StateChangedEvent("2", GreenLight)
    tested ! StateChangedEvent("1", RedLight)
    tested ! StateChangedEvent("2", RedLight)
    subscriber.probe.expectMsgType[StateChangedEvent] should be(StateChangedEvent("2", GreenLight))
    subscriber.probe.expectMsgType[StateChangedEvent] should be(StateChangedEvent("1", RedLight))
    subscriber.probe.expectMsgType[StateChangedEvent] should be(StateChangedEvent("2", RedLight))
    info("step 6: expect no message without explicitly requesting")
    tested ! StateChangedEvent("2", RedLight)
    tested ! StateChangedEvent("1", GreenLight)
    info("step 7: request next events and expect only last")
    subscription.request(3)
    tested ! StateChangedEvent("1", RedLight)
    expectNoMsg(200.millis)
    subscriber.probe.expectMsgType[StateChangedEvent] should be(StateChangedEvent("2", RedLight))
    subscriber.probe.expectMsgType[StateChangedEvent] should be(StateChangedEvent("1", GreenLight))
    subscriber.probe.expectMsgType[StateChangedEvent] should be(StateChangedEvent("1", RedLight))
    expectNoMsg(200.millis)
    info("step 8: cancel subscription")
    subscription.cancel()
    Thread.sleep(200)
    info("step 9: expect no more elements")
    tested ! StateChangedEvent("3", RedLight)
    tested ! StateChangedEvent("4", GreenLight)
    tested ! StateChangedEvent("5", RedLight)
    expectNoMsg(200.millis)
    info("step 10: expect monitoring to work as before")
    tested ! GetStatusQuery("4")
    val r1 = expectMsgType[Option[StateChangedEvent]]
    r1 should be(Some(StateChangedEvent("4", GreenLight)))
    expectNoMsg(200.millis)
  }

  it must "receive multiple GetPublisherQuery and send back separate publishers" in new MonitoringTest {
    info("step 1: request first publisher")
    tested ! GetPublisherQuery(_ => true)
    val p1 = expectMsgType[Publisher[StateChangedEvent]]
    info("step 2: request second publisher")
    tested ! GetPublisherQuery(_ => true)
    val p2 = expectMsgType[Publisher[StateChangedEvent]]
    p1 should not be (p2)
    info("step 3: register subscribers")
    val s1 = new TestSubscriber[StateChangedEvent]
    val s2 = new TestSubscriber[StateChangedEvent]
    p1.subscribe(s1)
    val subs1 = s1.probe.expectMsgType[Subscription]
    subs1.request(1000)
    p2.subscribe(s2)
    val subs2 = s2.probe.expectMsgType[Subscription]
    subs2.request(1000)
    info("step 4: expect same event sent to both subscribers")
    val e1 = StateChangedEvent("1", GreenLight)
    tested ! e1
    s1.probe.expectMsgType[StateChangedEvent] shouldBe e1
    s2.probe.expectMsgType[StateChangedEvent] shouldBe e1
    info("step 5: cancel first subscriber and expect events on second")
    subs1.cancel()
    Thread.sleep(200)
    val e2 = StateChangedEvent("1", RedLight)
    tested ! e2
    s2.probe.expectMsgType[StateChangedEvent] shouldBe e2
    s1.probe.expectNoMsg(200.millis)
  }

  it must "receive GetPublisherQuery and send events according to predicate" in new MonitoringTest {
    info("step 1: request publisher and register subscriber")
    tested ! GetPublisherQuery(id => id.contains("2"))
    val p1 = expectMsgType[Publisher[StateChangedEvent]]
    val s1 = new TestSubscriber[StateChangedEvent]
    p1.subscribe(s1)
    val subs1 = s1.probe.expectMsgType[Subscription]
    subs1.request(1000)
    info("step 2: expect only events having id matching predicate")
    tested ! StateChangedEvent("1", RedLight)
    s1.probe.expectNoMsg
    tested ! StateChangedEvent("2", GreenLight)
    s1.probe.expectMsgType[StateChangedEvent] should be(StateChangedEvent("2", GreenLight))
    tested ! StateChangedEvent("12", RedLight)
    s1.probe.expectMsgType[StateChangedEvent] should be(StateChangedEvent("12", RedLight))
    tested ! StateChangedEvent("3", RedLight)
    s1.probe.expectNoMsg(200.millis)
    info("step 3: cancel subscription")
    subs1.cancel()
    Thread.sleep(200)
    expectNoMsg(200.millis)
  }

  import publishers._

  "A Publisher Actor" must "receive subscription and call onSubscribe first (1.9)" in new ActorSystemTest {
    val subscriber = new TestSubscriber[String]
    val tested = new PublisherActor[String]
    tested.subscribe(subscriber)
    val subscription = subscriber.probe.expectMsgType[Subscription]
    subscription should not be null
  }

  it must "throw NullPointerException when subscriber is null (1.9)" in new ActorSystemTest {
    val tested = new PublisherActor[String]
    an[NullPointerException] should be thrownBy tested.subscribe(null)
  }

  it must "handle multiple different subscriptions" in new ActorSystemTest {
    val tested = new PublisherActor[Int]
    val s1 = new TestSubscriber[Int]
    val s2 = new TestSubscriber[Int]
    val s3 = new TestSubscriber[Int]
    tested.subscribe(s1)
    tested.subscribe(s2)
    tested.subscribe(s3)
    val sub1 = s1.probe.expectMsgType[Subscription]
    val sub2 = s2.probe.expectMsgType[Subscription]
    val sub3 = s3.probe.expectMsgType[Subscription]
    sub1 should not be sub2
    sub2 should not be sub3
  }

  it must "not allow duplicate subscription of the same subscriber" in new ActorSystemTest {
    val subs = new TestSubscriber[String]
    val tested = new PublisherActor[String]
    tested.subscribe(subs)
    tested.subscribe(subs)
    subs.probe.expectMsgType[Subscription]
    subs.probe.expectNoMsg(200.millis)
  }

  it must "handle subscription cancelling" in new ActorSystemTest {
    val subs = new TestSubscriber[String]
    val tested = new PublisherActor[String]
    tested.subscribe(subs)
    val subscription = subs.probe.expectMsgType[Subscription]
    subscription.cancel()
    subscription.cancel()
    subscription.cancel()
  }

  it must "forget subscriber when subscription has been cancelled" in new ActorSystemTest {
    val subs = new TestSubscriber[String]
    val tested = new PublisherActor[String]
    tested.subscribe(subs)
    val subscription1 = subs.probe.expectMsgType[Subscription]
    subscription1.cancel()
    tested.subscribe(subs)
    val subscription2 = subs.probe.expectMsgType[Subscription]
    subscription1 should not be subscription2
  }

  it must "allow to request elements multiple times" in new ActorSystemTest {
    val subs = new TestSubscriber[String]
    val tested = new PublisherActor[String]
    tested.subscribe(subs)
    val subscription1 = subs.probe.expectMsgType[Subscription]
    subscription1.request(1)
    subscription1.request(0)
    subscription1.request(-1)
    subscription1.request(10)
    subscription1.request(250000)
    subscription1.request(111)
    subscription1.request(19998776)
    subscription1.request(-245678)
    subscription1.cancel()
  }

  it must "buffer elements when not requested" in new ActorSystemTest {
    val subs = new TestSubscriber[String]
    val tested = new PublisherActor[String]
    tested.subscribe(subs)
    val subscription1 = subs.probe.expectMsgType[Subscription]
    tested.publish("abc")
    subs.probe.expectNoMsg(200.millis)
    tested.publish("abcd")
    subs.probe.expectNoMsg(200.millis)
    subscription1.request(2)
    subs.probe.expectMsg("abc")
    subs.probe.expectMsg("abcd")
    subscription1.request(2)
    tested.publish("bca")
    subs.probe.expectMsg("bca")
    tested.publish("abc")
    subs.probe.expectMsg("abc")
    tested.publish("_abc1234567890")
    subs.probe.expectNoMsg(200.millis)
    subscription1.request(2)
    subs.probe.expectMsg("_abc1234567890")
    tested.publish("bccda1_!")
    subscription1.request(1)
    subs.probe.expectMsg("bccda1_!")
  }

  it must "publish elements to subscriber" in new ActorSystemTest {
    val subs = new TestSubscriber[String]
    val tested = new PublisherActor[String]
    tested.subscribe(subs)
    val subscription1 = subs.probe.expectMsgType[Subscription]
    subscription1.request(100)
    tested.publish("abc")
    subs.probe.expectMsg("abc")
    tested.publish("abcd")
    subs.probe.expectMsg("abcd")
    tested.publish("bca")
    subs.probe.expectMsg("bca")
    tested.publish("abc")
    subs.probe.expectMsg("abc")
    tested.publish("_abc1234567890")
    subs.probe.expectMsg("_abc1234567890")
  }

  it must "broadcast elements to multiple subscribers" in new ActorSystemTest {
    val subs1 = new TestSubscriber[String]
    val subs2 = new TestSubscriber[String]
    val tested = new PublisherActor[String]
    tested.subscribe(subs1)
    tested.subscribe(subs2)
    val subscription1 = subs1.probe.expectMsgType[Subscription]
    val subscription2 = subs2.probe.expectMsgType[Subscription]
    subscription1.request(100)
    subscription2.request(10)
    tested.publish("abc")
    subs1.probe.expectMsg("abc")
    subs2.probe.expectMsg("abc")
    tested.publish("abcd")
    subs1.probe.expectMsg("abcd")
    subs2.probe.expectMsg("abcd")
    tested.publish("bca")
    subs1.probe.expectMsg("bca")
    subs2.probe.expectMsg("bca")
    tested.publish("abc")
    subs1.probe.expectMsg("abc")
    subs2.probe.expectMsg("abc")
    tested.publish("_abc1234567890")
    subs1.probe.expectMsg("_abc1234567890")
    subs2.probe.expectMsg("_abc1234567890")
  }

  it must "not broadcast elements to the already cancelled subscribers" in new ActorSystemTest {
    val subs1 = new TestSubscriber[String]
    val subs2 = new TestSubscriber[String]
    val tested = new PublisherActor[String]
    tested.subscribe(subs1)
    tested.subscribe(subs2)
    val subscription1 = subs1.probe.expectMsgType[Subscription]
    val subscription2 = subs2.probe.expectMsgType[Subscription]
    subscription1.request(100)
    subscription2.request(10)
    tested.publish("abc")
    subs1.probe.expectMsg("abc")
    subs2.probe.expectMsg("abc")
    tested.publish("abcd")
    subs1.probe.expectMsg("abcd")
    subs2.probe.expectMsg("abcd")
    subscription1.cancel()
    tested.publish("bca")
    subs1.probe.expectNoMsg(100.millis)
    subs2.probe.expectMsg("bca")
    tested.publish("abc")
    subs1.probe.expectNoMsg(100.millis)
    subs2.probe.expectMsg("abc")
    subscription2.cancel()
    tested.publish("_abc1234567890")
    subs1.probe.expectNoMsg(100.millis)
    subs2.probe.expectNoMsg(100.millis)
  }

}
