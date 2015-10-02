package trafficlightscontrol.http

import java.nio.charset.Charset
import java.nio.file.{ FileSystems, Files, Path }
import java.util.function.Consumer

import org.scalatest.{ Finders, FlatSpecLike, Matchers }
import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestActorRef, TestKit }
import org.scalatest.junit.JUnitRunner
import com.typesafe.config.ConfigFactory
import akka.testkit.TestProbe
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import java.util.concurrent.atomic.AtomicInteger
import akka.actor.ActorRef

import trafficlightscontrol.actors._
import trafficlightscontrol.model._

import org.reactivestreams.{ Publisher, Subscriber, Subscription }

class MonitoringSpec extends FlatSpecLike with Matchers with ActorSystemTestKit {

  class MonitoringTest extends ActorSystemTest {
    val trafficSystem = TestProbe()
    val tested = TestActorRef(new MonitoringActor())
  }

  "A Monitoring actor" must "receive StatusEvent and register current state of light" in new MonitoringTest {
    tested ! StatusEvent("1", RedLight)
    tested.underlyingActor.report("1") should equal(RedLight)
    tested ! StatusEvent("1", GreenLight)
    tested.underlyingActor.report("1") should equal(GreenLight)
    tested ! StatusEvent("2", RedLight)
    tested.underlyingActor.report("1") should equal(GreenLight)
    tested.underlyingActor.report("2") should equal(RedLight)
    tested ! StatusEvent("2", GreenLight)
    tested.underlyingActor.report("1") should equal(GreenLight)
    tested.underlyingActor.report("2") should equal(GreenLight)
    tested ! StatusEvent("1", RedLight)
    tested.underlyingActor.report("1") should equal(RedLight)
    tested.underlyingActor.report("2") should equal(GreenLight)
  }

  it must "receive GetReportQuery and send back current system status report as a ReportEvent" in new MonitoringTest {
    info("case 1: when none event yet happened")
    tested ! GetReportQuery
    val r1 = expectMsgAnyClassOf(classOf[ReportEvent])
    r1.report should have size 0

    info("case 2: when light #1 became red")
    tested ! StatusEvent("1", RedLight)
    tested ! GetReportQuery
    val r2 = expectMsgAnyClassOf(classOf[ReportEvent])
    r2 should not be r1
    r2.report should have size 1
    r2.report.get("1") should be(Some(RedLight))

    info("case 3: when light #1 became green")
    tested ! StatusEvent("1", GreenLight)
    tested ! GetReportQuery
    val r3 = expectMsgAnyClassOf(classOf[ReportEvent])
    r3.report should have size 1
    r3.report.get("1") should be(Some(GreenLight))

    info("case 4: when light #2 became red")
    tested ! StatusEvent("2", RedLight)
    tested ! GetReportQuery
    val r4 = expectMsgAnyClassOf(classOf[ReportEvent])
    r4.report should have size 2
    r4.report.get("1") should be(Some(GreenLight))
    r4.report.get("2") should be(Some(RedLight))

    info("case 5: when light #2 became green")
    tested ! StatusEvent("2", GreenLight)
    tested ! GetReportQuery
    val r5 = expectMsgAnyClassOf(classOf[ReportEvent])
    r5.report should have size 2
    r5.report.get("1") should be(Some(GreenLight))
    r5.report.get("2") should be(Some(GreenLight))

    info("case 6: when light #1 became again red")
    tested ! StatusEvent("1", RedLight)
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

  it must "receive GetStatusQuery(id=1) and send back light status as an Option[StatusEvent]" in new MonitoringTest {
    info("case 1: when light #1 state is not known yet")
    tested ! GetStatusQuery("1")
    val r1 = expectMsgAnyClassOf(classOf[Option[StatusEvent]])
    r1 should be(None)

    info("case 2: when light #2 state but not #1 has been reported")
    tested ! StatusEvent("2", GreenLight)
    tested ! GetStatusQuery("1")
    val r2 = expectMsgAnyClassOf(classOf[Option[StatusEvent]])
    r2 should be(None)

    info("case 3: when light #1 state is already known")
    tested ! StatusEvent("1", GreenLight)
    tested ! GetStatusQuery("1")
    val r3 = expectMsgAnyClassOf(classOf[Option[StatusEvent]])
    r3 should be(Some(StatusEvent("1", GreenLight)))
  }

  it must "receive GetPublisherQuery and send back Publisher[StatusEvent]" in new MonitoringTest {
    info("step 1: request publisher")
    tested ! GetPublisherQuery(_ => true)
    val publisher = expectMsgAnyClassOf(classOf[Publisher[StatusEvent]])
    info("step 2: subscribe for events")
    val subscriber = new TestSubscriber[StatusEvent]
    publisher.subscribe(subscriber)
    val subscription = subscriber.probe.expectMsgType[Subscription]
    info("step 3: request 5 events and expect StatusEvent for light #1")
    tested ! StatusEvent("1", GreenLight)
    subscription.request(5)
    subscriber.probe.expectMsgType[StatusEvent] should be(StatusEvent("1", GreenLight))
    info("step 4: expect StatusEvent for light #2")
    tested ! StatusEvent("2", RedLight)
    subscriber.probe.expectMsgType[StatusEvent] should be(StatusEvent("2", RedLight))
    info("step 5: expect 3 more StatusEvents")
    tested ! StatusEvent("2", GreenLight)
    tested ! StatusEvent("1", RedLight)
    tested ! StatusEvent("2", RedLight)
    subscriber.probe.expectMsgType[StatusEvent] should be(StatusEvent("2", GreenLight))
    subscriber.probe.expectMsgType[StatusEvent] should be(StatusEvent("1", RedLight))
    subscriber.probe.expectMsgType[StatusEvent] should be(StatusEvent("2", RedLight))
    info("step 6: expect no message without explicitly requesting")
    tested ! StatusEvent("2", RedLight)
    tested ! StatusEvent("1", GreenLight)
    tested ! StatusEvent("1", RedLight)
    subscriber.probe.expectNoMsg
    info("step 7: request next events and expect only last")
    subscription.request(5)
    subscriber.probe.expectMsgType[StatusEvent] should be(StatusEvent("1", RedLight))
    subscriber.probe.expectNoMsg
    info("step 8: cancel subscription")
    subscription.cancel()
    Thread.sleep(200)
    info("step 9: expect no more elements")
    tested ! StatusEvent("3", RedLight)
    tested ! StatusEvent("4", GreenLight)
    tested ! StatusEvent("5", RedLight)
    subscriber.probe.expectNoMsg
    info("step 10: expect monitoring to work as before")
    tested ! GetStatusQuery("4")
    val r1 = expectMsgType[Option[StatusEvent]]
    r1 should be(Some(StatusEvent("4", GreenLight)))
    expectNoMsg
  }

  it must "receive multiple GetPublisherQuery and send back separate publishers" in new MonitoringTest {
    info("step 1: request first publisher")
    tested ! GetPublisherQuery(_ => true)
    val p1 = expectMsgType[Publisher[StatusEvent]]
    info("step 2: request second publisher")
    tested ! GetPublisherQuery(_ => true)
    val p2 = expectMsgType[Publisher[StatusEvent]]
    p1 should not be (p2)
    info("step 3: register subscribers")
    val s1 = new TestSubscriber[StatusEvent]
    val s2 = new TestSubscriber[StatusEvent]
    p1.subscribe(s1)
    val subs1 = s1.probe.expectMsgType[Subscription]
    subs1.request(1000)
    p2.subscribe(s2)
    val subs2 = s2.probe.expectMsgType[Subscription]
    subs2.request(1000)
    info("step 4: expect same event sent to both subscribers")
    val e1 = StatusEvent("1", GreenLight)
    tested ! e1
    s1.probe.expectMsgType[StatusEvent] shouldBe e1
    s2.probe.expectMsgType[StatusEvent] shouldBe e1
    info("step 5: cancel first subscriber and expect events on second")
    subs1.cancel()
    Thread.sleep(200)
    val e2 = StatusEvent("1", RedLight)
    tested ! e2
    s2.probe.expectMsgType[StatusEvent] shouldBe e2
    s1.probe.expectNoMsg
  }

  it must "receive GetPublisherQuery and send events according to predicate" in new MonitoringTest {
    info("step 1: request publisher and register subscriber")
    tested ! GetPublisherQuery(id => id.contains("2"))
    val p1 = expectMsgType[Publisher[StatusEvent]]
    val s1 = new TestSubscriber[StatusEvent]
    p1.subscribe(s1)
    val subs1 = s1.probe.expectMsgType[Subscription]
    subs1.request(1000)
    info("step 2: expect only events having id matching predicate")
    tested ! StatusEvent("1", RedLight)
    s1.probe.expectNoMsg
    tested ! StatusEvent("2", GreenLight)
    s1.probe.expectMsgType[StatusEvent] should be(StatusEvent("2", GreenLight))
    tested ! StatusEvent("12", RedLight)
    s1.probe.expectMsgType[StatusEvent] should be(StatusEvent("12", RedLight))
    tested ! StatusEvent("3", RedLight)
    s1.probe.expectNoMsg
    info("step 3: cancel subscription")
    subs1.cancel()
    Thread.sleep(200)
    expectNoMsg
  }

}
