package trafficlightscontrol.actors

import com.typesafe.config.ConfigFactory
import akka.testkit.TestKit
import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestProbe
import org.scalatest.BeforeAndAfter
import org.scalatest.Suite
import akka.testkit.TestKitBase
import org.scalatest.BeforeAndAfterAll
import scala.concurrent.duration._

import trafficlightscontrol.model._

trait ActorSystemTestKit extends BeforeAndAfterAll { this: Suite =>

  private val actorSystemConfig = ConfigFactory.load
  val actorSystem = ActorSystem("test", actorSystemConfig)
  val checkTimeout: FiniteDuration = 30.seconds

  class ActorSystemTest extends TestKit(actorSystem) with ImplicitSender {

    val eventListener = TestProbe()
    actorSystem.eventStream.subscribe(eventListener.ref, classOf[StatusEvent])

  }

  override def afterAll() {
    Thread.sleep(100)
    TestKit.shutdownActorSystem(actorSystem)
  }
}
