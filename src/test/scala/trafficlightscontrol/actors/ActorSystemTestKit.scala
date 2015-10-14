package trafficlightscontrol.actors

import com.typesafe.config.ConfigFactory
import akka.testkit.TestKit
import akka.actor.{ Actor, ActorSystem, ActorRef, Props }
import akka.testkit.ImplicitSender
import akka.testkit.TestProbe
import org.scalatest.BeforeAndAfter
import org.scalatest.Suite
import akka.testkit.TestKitBase
import org.scalatest.BeforeAndAfterAll
import scala.concurrent.duration._

import org.reactivestreams.{ Publisher, Subscriber, Subscription }

import trafficlightscontrol.model._

trait ActorSystemTestKit extends BeforeAndAfterAll { this: Suite =>

  private val actorSystemConfig = ConfigFactory.load
  val actorSystem = ActorSystem("test", actorSystemConfig)
  val checkTimeout: FiniteDuration = 30.seconds

  class ActorSystemTest extends TestKit(actorSystem) with ImplicitSender {

    val eventListener = TestProbe()
    actorSystem.eventStream.subscribe(eventListener.ref, classOf[StatusEvent])

  }

  class TestSubscriber[T](implicit val system: ActorSystem) extends Subscriber[T] {
    val probe = TestProbe()
    def onComplete(): Unit = { probe.ref ! "Completed" }
    def onError(error: Throwable): Unit = { probe.ref ! error }
    def onNext(element: T): Unit = { probe.ref ! element }
    def onSubscribe(subscription: Subscription): Unit = { probe.ref ! subscription }
  }

  override def afterAll() {
    Thread.sleep(100)
    TestKit.shutdownActorSystem(actorSystem)
  }
}

object TestProps {
  def apply(probe: TestProbe): Props = Props(new Actor {
    val receive: Receive = {
      case message => probe.ref forward message
    }
  })
}

case class MessageEcho(id: String, message: Any)

object ProxyTestProps {
  def apply(probe: TestProbe, props: Props, id: String): Props = Props(new Actor {
    val target = context.actorOf(props)
    val receive: Receive = {
      case message =>
        target forward message
        probe.ref forward MessageEcho(id, message)
    }
  })

}
