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
class TrafficDirectorSpec extends FlatSpecLike with Matchers with ActorSystemTestKit with TrafficSystemTestKit {

  "A TrafficLight actor" should "change status from red to green" in new ActorSystemTest {
    val tested = TestTrafficLight()
    tested ! ChangeToGreenCommand("1")
    expectMsg(ChangedToGreenEvent("1"))
  }

}
