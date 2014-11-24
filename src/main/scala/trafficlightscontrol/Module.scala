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

class Module(implicit system: ActorSystem) extends Macwire {

  val period: FiniteDuration = 10.seconds

  lazy val demoTrafficActor = ActorOf("traffic", classOf[DemoTrafficSystem])
  lazy val monitoringActor = ActorOf("monitoring", classOf[MonitoringActor], demoTrafficActor, period)
  lazy val httpServiceActor = ActorOf("httpservice", classOf[HttpServiceActor], monitoringActor)

}

object ActorOf {
  def apply(name: String, actorClass: Class[_], args: Any*)(implicit factory: ActorRefFactory): ActorRef = factory.actorOf(Props(actorClass, args: _*), name)
}
