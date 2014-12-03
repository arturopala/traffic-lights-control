package trafficlightscontrol

import akka.testkit.TestActorRef
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.actor.ActorRef

trait TrafficSystemTestKit {

  object TestTrafficLight {
    def apply(id: String = "1", status: Light = RedLight, delay: FiniteDuration = 100 milliseconds)(implicit system: ActorSystem) =
      TestActorRef(new TrafficLight(id, status, delay))
  }

  object TestTrafficDetector {
    def apply()(implicit system: ActorSystem) = {
      TestActorRef(new TrafficDetector(""))
    }
  }

  object TestLightManager {
    def apply(lights: Seq[Light], timeout: FiniteDuration = 10 seconds)(implicit system: ActorSystem) = {
      val workers = lights zip (1 to lights.size) map { case (l, c) => ("" + c -> TestTrafficLight("" + c, l)) }
      TestActorRef(new LightsGroupWithOnlyOneIsGreenStrategy(workers.toMap, timeout))
    }
  }

  object TestTrafficDirector {
    def apply(lights: Seq[Light], timeout: FiniteDuration = 10 seconds)(implicit system: ActorSystem) = {
      val lightManager = TestLightManager(lights, timeout)
      val detectors = lights zip (1 to lights.size) map { case (l, c) => (TestTrafficDetector(), "" + c) }
      TestActorRef(new TrafficDirector(lightManager, Set(detectors: _*)))
    }
  }

  def statusOfTrafficLight(ref: ActorRef): Light = ref.asInstanceOf[TestActorRef[TrafficLight]].underlyingActor.status

}
