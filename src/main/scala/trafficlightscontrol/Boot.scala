package trafficlightscontrol

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import trafficlightscontrol.model._

import scala.concurrent.duration._

object Boot extends App {

  import system.dispatcher // for the future transformations
  implicit val futureTimeout = Timeout(10.seconds)

  implicit val system = ActorSystem("app")
  implicit val materializer = ActorMaterializer()
  implicit val module = new Module

  val conf = ConfigFactory.load()
  val host = conf.getString("application.http.host")
  val port = conf.getInt("application.http.port")

  val httpBinding = Http().bindAndHandle(module.httpService.route, host, port)

  module.manager.actor ! StartSystemCommand("demo")

  println(s"Started server on $host:$port, press RETURN to stop...")
  scala.io.StdIn.readLine()

  httpBinding
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ ⇒ system.terminate()) // and terminate when done
}
