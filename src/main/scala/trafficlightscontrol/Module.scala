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

  val demoTrafficActor: ActorRef = actorOf[DemoTrafficSystem]("demo", period)
  lazy val monitoring: Monitoring = Monitoring(actorOf[MonitoringActor]("monitor"))
  lazy val httpService: TrafficHttpService = wire[TrafficHttpService]
  lazy val httpServiceActor: ActorRef = actorOf[TrafficHttpServiceActor]("http", httpService)

  lazy val webSocketRoute: ws.Route[ActorRef] = ws.Routes(
    "/light/status/stream" -> monitoring.actor)
  lazy val webSocketServiceActor: ActorRef = actorOf[WebSocketServiceActor]("websocket", webSocketRoute, httpServiceActor)

}

trait ActorOf {
  def actorOf[T](name: String, args: Any*)(implicit factory: ActorRefFactory, ct: ClassTag[T]): ActorRef = factory.actorOf(Props(ct.runtimeClass, args: _*), name)
}
