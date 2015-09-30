package trafficlightscontrol

import scala.concurrent.duration.DurationInt
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object Boot extends App {

  implicit val system = ActorSystem("app")
  implicit val materializer = akka.stream.ActorMaterializer()
  implicit val module = new Module

  val httpBinding = module.httpService.bind("localhost", 8080)

  module.demoTrafficActor ! "Start"

  println("Press RETURN to stop...")
  scala.io.StdIn.readLine()
  import system.dispatcher // for the future transformations
  httpBinding
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ ⇒ system.shutdown()) // and shutdown when done
}
