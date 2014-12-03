package trafficlightscontrol

import com.typesafe.config.ConfigFactory
import akka.testkit.TestKit
import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestProbe
import org.scalatest.BeforeAndAfter
import org.scalatest.Suite
import akka.testkit.TestKitBase
import org.scalatest.BeforeAndAfterAll

trait ActorSystemTestKit extends BeforeAndAfterAll { this: Suite =>

  private val config = """
                 |akka.log-dead-letters = 0
                 |akka.log-dead-letters-during-shutdown = off
               """.stripMargin
  private val actorSystemConfig = ConfigFactory.parseString(config).withFallback(ConfigFactory.load)
  val actorSystem = ActorSystem("test", actorSystemConfig)

  class ActorSystemTest extends TestKit(actorSystem) with ImplicitSender

  override def afterAll() {
    Thread.sleep(100)
    TestKit.shutdownActorSystem(actorSystem)
  }
}
