package trafficlightscontrol

import akka.testkit.TestActorRef
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.actor.ActorRef

trait TrafficSystemTestKit {

  val testLightChangeDelay: FiniteDuration = 100 milliseconds

  object TestTrafficLight {
    def apply(id: String = "1", initialState: Light = RedLight, delay: FiniteDuration = testLightChangeDelay)(implicit system: ActorSystem) =
      TestActorRef(new TrafficLight(id, initialState, delay))
  }

  object TestTrafficLightFSM {
    def apply(id: String = "1", initialState: Light = RedLight, delay: FiniteDuration = testLightChangeDelay)(implicit system: ActorSystem) =
      TestActorRef(new TrafficLightFSM(id, initialState, delay))
  }

  object TestTrafficDetector {
    def apply()(implicit system: ActorSystem) = {
      TestActorRef(new TrafficDetector(""))
    }
  }

  object TestLightManager {
    def apply(lights: Seq[Light], timeout: FiniteDuration = 10 seconds)(implicit system: ActorSystem) = {
      val workers = lights zip (1 to lights.size) map { case (l, c) => ("" + c -> TestTrafficLight("" + c, l)) }
      TestActorRef(new OnlyOneIsGreenSwitch(workers.toMap, timeout))
    }
  }

  object TestTrafficDirector {
    def apply(lights: Seq[Light], timeout: FiniteDuration = 10 seconds)(implicit system: ActorSystem) = {
      val lightManager = TestLightManager(lights, timeout)
      val detectors = lights zip (1 to lights.size) map { case (l, c) => (TestTrafficDetector(), "" + c) }
      TestActorRef(new TrafficDirector(lightManager, Set(detectors: _*)))
    }
  }

  def stateOfTrafficLight(ref: ActorRef): Light = ref.asInstanceOf[TestActorRef[TrafficLight]].underlyingActor.state

}
