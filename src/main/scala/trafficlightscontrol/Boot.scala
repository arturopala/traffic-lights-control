package trafficlightscontrol

import scala.concurrent.duration.DurationInt
import akka.actor.ActorSystem
import akka.io.IO
import akka.util.Timeout
import spray.can.Http
import spray.can.server.UHttp

object Boot extends App {

  implicit val system = ActorSystem("app")
  implicit val module = new Module {}

  implicit val timeout = Timeout(5.seconds)
  IO(UHttp) ! Http.Bind(module.webSocketServiceActor, interface = "0.0.0.0", port = 8080)
}
