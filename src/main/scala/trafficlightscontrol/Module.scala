package trafficlightscontrol

import scala.reflect.ClassTag

import com.softwaremill.macwire.Macwire
import akka.actor.Props
import scala.concurrent.duration._
import akka.actor.IndirectActorProducer
import akka.actor.Actor
import akka.actor.ActorContext
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.ActorRefFactory

import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http

import trafficlightscontrol.actors._, trafficlightscontrol.http._

class Module(implicit system: ActorSystem, materializer: ActorMaterializer) extends Macwire with ActorOf {

  val interval: FiniteDuration = 5.seconds
  lazy val manager: ActorRef = actorOf[TrafficSystemsManager]("manager")
  lazy val monitoring: Monitoring = Monitoring(actorOf[MonitoringActor]("monitoring"))
  lazy val httpService: HttpService = wire[HttpService]

  //lazy val httpService: TrafficHttpService = wire[TrafficHttpService]
  //lazy val httpServiceActor: ActorRef = actorOf[TrafficHttpServiceActor]("http", httpService)
  /*lazy val webSocketRoute: ws.Route[ActorRef] = ws.Routes(
    "/light/status/stream" -> monitoring.actor)
  lazy val webSocketServiceActor: ActorRef = actorOf[WebSocketServiceActor]("websocket", webSocketRoute, httpServiceActor)*/

}

trait ActorOf {
  def actorOf[T](name: String, args: Any*)(implicit factory: ActorRefFactory, ct: ClassTag[T]): ActorRef = factory.actorOf(Props(ct.runtimeClass, args: _*), name)
}
