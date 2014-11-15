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
class TrafficDirectorSpec extends FlatSpecLike with Matchers {

  val config = """
                 |akka.log-dead-letters = 0
                 |akka.log-dead-letters-during-shutdown = off
               """.stripMargin

  val actorSystemConfig = ConfigFactory.parseString(config).withFallback(ConfigFactory.load)

  object TestTrafficLight {
    def apply(id: String = "1", status: Light = RedLight, delay: FiniteDuration = 100 milliseconds)(implicit system: ActorSystem) =
      TestActorRef(new TrafficLight(id, status, delay))
  }

  object TestTrafficDetector {
    def apply()(implicit system: ActorSystem) = {
      TestActorRef(new TrafficDetector(""))
    }
  }

  object TestLightManager {
    def apply(lights: Seq[Light], timeout: FiniteDuration = 10 seconds)(implicit system: ActorSystem) = {
      val workers = lights zip (1 to lights.size) map { case (l, c) => ("" + c -> TestTrafficLight("" + c, l)) }
      TestActorRef(new LightsGroupWithOnlyOneIsGreenStrategy(workers.toMap, timeout))
    }
  }

  object TestTrafficDirector {
    def apply(lights: Seq[Light], timeout: FiniteDuration = 10 seconds)(implicit system: ActorSystem) = {
      val lightManager = TestLightManager(lights, timeout)
      val detectors = lights zip (1 to lights.size) map { case (l, c) => (TestTrafficDetector(), "" + c) }
      TestActorRef(new TrafficDirector(lightManager, Set(detectors: _*)))
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

}
