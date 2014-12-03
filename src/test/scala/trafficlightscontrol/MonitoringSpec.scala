package trafficlightscontrol

import java.nio.charset.Charset
import java.nio.file.{ FileSystems, Files, Path }
import java.util.function.Consumer
import org.junit.runner.RunWith
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

@RunWith(classOf[JUnitRunner])
class MonitoringSpec extends FlatSpecLike with Matchers with ActorSystemTestKit {

  class MonitoringTest extends ActorSystemTest {
    val trafficSystem = TestProbe()
    val tested = TestActorRef(new MonitoringActor(trafficSystem.ref))
  }

  "A Monitoring actor" should "register status events" in new MonitoringTest {
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

  it should "register websocket listeners" in new MonitoringTest {
    val probe1 = TestProbe()
    tested ! ws.Open("/", probe1.ref)
    tested.underlyingActor.listeners should contain(probe1.ref)
    val probe2 = TestProbe()
    tested ! ws.Open("/", probe2.ref)
    tested.underlyingActor.listeners should contain(probe1.ref)
    tested.underlyingActor.listeners should contain(probe2.ref)
    tested ! ws.Open("/", probe1.ref)
    tested.underlyingActor.listeners should contain(probe1.ref)
    tested.underlyingActor.listeners should contain(probe2.ref)
  }

  it should "watch registered listeners" in new MonitoringTest {
    val probe = TestProbe()
    tested ! ws.Open("/", probe.ref)
    tested.underlyingActor.listeners should contain(probe.ref)
    system.stop(probe.ref)
    awaitAssert(tested.underlyingActor.listeners should not contain (probe.ref), 5.seconds, 500.millis)

  }

}
