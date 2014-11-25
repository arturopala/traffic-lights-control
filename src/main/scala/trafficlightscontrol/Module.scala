package trafficlightscontrol

import com.softwaremill.macwire.Macwire
import akka.actor.Props
import scala.concurrent.duration._
import akka.actor.IndirectActorProducer
import akka.actor.Actor
import akka.actor.ActorContext
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.ActorRefFactory
import scala.reflect.ClassTag

class Module(implicit system: ActorSystem) extends Macwire with ActorOf {

  val period: FiniteDuration = 10.seconds

  lazy val demoTrafficActor: ActorRef = actorOf[DemoTrafficSystem]("demo", period)
  lazy val monitoringActor: ActorRef = actorOf[MonitoringActor]("monitoring", demoTrafficActor)
  lazy val httpServiceActor: ActorRef = actorOf[HttpServiceActor]("http", monitoringActor)
  lazy val webServiceActor: ActorRef = actorOf[WebServiceActor]("ws")
  lazy val webSocketServiceActor: ActorRef = actorOf[WebSocketServiceActor]("websocket", webServiceActor, httpServiceActor)

}

trait ActorOf {
  def actorOf[T](name: String, args: Any*)(implicit factory: ActorRefFactory, ct: ClassTag[T]): ActorRef = factory.actorOf(Props(ct.runtimeClass, args: _*), name)
}
