package trafficlightscontrol

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import scala.concurrent.duration._
import trafficlightscontrol.model._

object Boot extends App {

  import system.dispatcher // for the future transformations
  implicit val futureTimeout = Timeout(10.seconds)

  implicit val system = ActorSystem("app")
  implicit val materializer = akka.stream.ActorMaterializer()
  implicit val module = new Module

  val httpBinding = module.httpService.bind("localhost", 8080)

  module.manager.actor ! StartSystemCommand("demo")

  println("Press RETURN to stop...")
  scala.io.StdIn.readLine()

  httpBinding
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ ⇒ system.shutdown()) // and shutdown when done
}
