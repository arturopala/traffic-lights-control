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

@RunWith(classOf[JUnitRunner])
class TrafficLightsSpec extends FlatSpecLike with Matchers {

  val config = """
  |akka.log-dead-letters = 0
  |akka.log-dead-letters-during-shutdown = off
    """.stripMargin

  val actorSystemConfig = ConfigFactory.parseString(config).withFallback(ConfigFactory.load)

  object TestTrafficLight {
    val counter: AtomicInteger = new AtomicInteger(1)
    def apply(id: String = s"Light#${counter.getAndIncrement}", status: Light = RedLight, delay: FiniteDuration = 100 milliseconds)(implicit system: ActorSystem, executionContext: ExecutionContext) =
      TestActorRef(new TrafficLight(id, status, delay))
  }

  abstract class ActorsTest extends TestKit(ActorSystem("test", actorSystemConfig)) with ImplicitSender {
    implicit val executionContext: ExecutionContext = system.dispatcher

    def clean(implicit system: ActorSystem): Unit = {
      Thread.sleep(100)
      TestKit.shutdownActorSystem(system)
    }
  }

  "A TrafficLight actor" should "change status from red to green" in new ActorsTest {
    val tested = TestTrafficLight()
    tested ! ChangeToGreenCommand
    expectMsg(ChangedToGreenEvent)
    clean
  }

  it should "change status from green to red" in new ActorsTest {
    val tested = TestTrafficLight(status = GreenLight)
    tested ! ChangeToRedCommand
    expectMsg(ChangedToRedEvent)
    clean
  }

  it should "return current status" in new ActorsTest {
    val greenLight = TestTrafficLight(status = GreenLight)
    val redLight = TestTrafficLight(status = RedLight)
    greenLight ! GetStatusQuery
    expectMsg(GreenLight)
    redLight ! GetStatusQuery
    expectMsg(RedLight)
    clean
  }

  //  it should "first change to orange and then to red"
}
