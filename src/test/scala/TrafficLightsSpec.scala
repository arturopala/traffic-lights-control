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
class TrafficLightsSpec extends FlatSpecLike with Matchers {

  val config = """
  |akka.log-dead-letters = 0
  |akka.log-dead-letters-during-shutdown = off
    """.stripMargin

  val actorSystemConfig = ConfigFactory.parseString(config).withFallback(ConfigFactory.load)

  object TestTrafficLight {
    def apply(id: String = "1", status: Light = RedLight, delay: FiniteDuration = 100 milliseconds)(implicit system: ActorSystem) =
      TestActorRef(new TrafficLight(id, status, delay))
  }

  object TestLightManager {
    def apply(lights: Seq[Light], timeout: FiniteDuration = 10 seconds)(implicit system: ActorSystem) = {
      val workers = lights zip (1 to lights.size) map { case (l, c) => ("" + c -> TestTrafficLight("" + c, l)) }
      TestActorRef(new LightsGroupWithOnlyOneIsGreenStrategy(workers.toMap, timeout))
    }
  }

  abstract class ActorsTest extends TestKit(ActorSystem("test", actorSystemConfig)) with ImplicitSender {

    def clean(implicit system: ActorSystem): Unit = {
      Thread.sleep(100)
      TestKit.shutdownActorSystem(system)
    }
  }

  "A TrafficLight actor" should "change status from red to green" in new ActorsTest {
    val tested = TestTrafficLight()
    val probe = TestProbe()
    implicit val sender = probe.ref
    tested ! ChangeToGreenCommand("1")
    probe.expectMsg(ChangedToGreenEvent("1"))
    clean
  }

  it should "change status from green to red" in new ActorsTest {
    val tested = TestTrafficLight(status = GreenLight)
    tested ! ChangeToRedCommand
    expectMsg(ChangedToRedEvent)
    clean
  }

  it should "return current status" in new ActorsTest {
    val greenLight = TestTrafficLight(id = "A", status = GreenLight)
    val redLight = TestTrafficLight(id = "B", status = RedLight)
    greenLight ! GetStatusQuery
    expectMsg(StatusEvent("A", GreenLight))
    redLight ! GetStatusQuery
    expectMsg(StatusEvent("B", RedLight))
    clean
  }

  "A LightsGroupWithOnlyOneIsGreenStrategy actor" should "change status of Light#1 from red to green" in new ActorsTest {
    val tested = TestLightManager(Seq(RedLight, RedLight, RedLight, RedLight))
    val probe = TestProbe()
    implicit val sender = probe.ref
    tested ! ChangeToGreenCommand("1")
    probe.expectMsg(ChangedToGreenEvent("1"))
    clean
  }

  def statusOf(ref: ActorRef): Light = ref.asInstanceOf[TestActorRef[TrafficLight]].underlyingActor.status

  it should "change status of Light#1 from red to green and status of others to red" in new ActorsTest {
    val tested = TestLightManager(Seq(RedLight, GreenLight, RedLight, RedLight))
    val workers = tested.underlyingActor.workers
    val probe = TestProbe()
    implicit val sender = probe.ref
    statusOf(workers("1")) should equal(RedLight)
    statusOf(workers("2")) should equal(GreenLight)
    statusOf(workers("3")) should equal(RedLight)
    statusOf(workers("4")) should equal(RedLight)

    tested ! ChangeToGreenCommand("1")

    probe.expectMsg(ChangedToGreenEvent("1"))
    statusOf(workers("1")) should equal(GreenLight)
    statusOf(workers("2")) should equal(RedLight)
    statusOf(workers("3")) should equal(RedLight)
    statusOf(workers("4")) should equal(RedLight)
    clean
  }

  it should "change status of Light#1 from red to green and status of others to red (2)" in new ActorsTest {
    val tested = TestLightManager(Seq(RedLight, GreenLight, GreenLight, GreenLight))
    val workers = tested.underlyingActor.workers
    val probe = TestProbe()
    implicit val sender = probe.ref
    statusOf(workers("1")) should equal(RedLight)
    statusOf(workers("2")) should equal(GreenLight)
    statusOf(workers("3")) should equal(GreenLight)
    statusOf(workers("4")) should equal(GreenLight)

    tested ! ChangeToGreenCommand("1")

    probe.expectMsg(ChangedToGreenEvent("1"))
    statusOf(workers("1")) should equal(GreenLight)
    statusOf(workers("2")) should equal(RedLight)
    statusOf(workers("3")) should equal(RedLight)
    statusOf(workers("4")) should equal(RedLight)
    clean
  }

  it should "change status of all lights to red" in new ActorsTest {
    val tested = TestLightManager(Seq(GreenLight, GreenLight, RedLight, GreenLight))
    val workers = tested.underlyingActor.workers
    val probe = TestProbe()
    implicit val sender = probe.ref
    statusOf(workers("1")) should equal(GreenLight)
    statusOf(workers("2")) should equal(GreenLight)
    statusOf(workers("3")) should equal(RedLight)
    statusOf(workers("4")) should equal(GreenLight)

    tested ! ChangeToRedCommand

    probe.expectMsg(ChangedToRedEvent)
    statusOf(workers("1")) should equal(RedLight)
    statusOf(workers("2")) should equal(RedLight)
    statusOf(workers("3")) should equal(RedLight)
    statusOf(workers("4")) should equal(RedLight)
    clean
  }

}
