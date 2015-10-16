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

  val interval = 10.seconds

  implicit val config = Configuration(
    interval = interval,
    delayRedToGreen = interval / 4,
    delayGreenToRed = interval / 6,
    sequenceDelay = interval / 10,
    timeout = interval * 10
  )

  val demoLayout =

    Sequence("s1", SequenceStrategy.RoundRobin,
      Group(
        "g1",
        Light("l1", RedLight),
        Offset(
          500.millis,
          Light("l2", GreenLight)
        )
      ),

      Group(
        "g2",
        Light("l3", GreenLight),
        Offset(
          1.second,
          Light("l4", RedLight)
        )
      ),

      Group(
        "g3",
        Light("l5", GreenLight),
        Offset(
          1.second,
          Light("l6", RedLight)
        ),
        Sequence("s2", SequenceStrategy.RoundRobin,
          Light("l7", RedLight),
          Light("l8", RedLight),
          Light("l9", RedLight))
      ))

  val demoId = "demo"

  module.manager ? InstallComponentCommand(demoLayout, demoId) onSuccess {
    case InstallComponentSucceededEvent(_, _) => module.manager ! StartSystemCommand(demoId)
  }

  println("Press RETURN to stop...")
  scala.io.StdIn.readLine()

  httpBinding
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ ⇒ system.shutdown()) // and shutdown when done
}
