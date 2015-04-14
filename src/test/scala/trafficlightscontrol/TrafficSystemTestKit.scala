package trafficlightscontrol

import akka.testkit.TestActorRef
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.actor.ActorRef

trait TrafficSystemTestKit {

  val testLightChangeDelay: FiniteDuration = 100 milliseconds

  object TestTrafficLight {
    def apply(id: String = "1", initialState: LightState = RedLight, delay: FiniteDuration = testLightChangeDelay)(implicit system: ActorSystem) =
      TestActorRef(new TrafficLight(id, initialState, delay))
  }

  object TestTrafficLightFSM {
    def apply(id: String = "1", initialState: LightState = RedLight, delay: FiniteDuration = testLightChangeDelay)(implicit system: ActorSystem) =
      TestActorRef(new TrafficLightFSM(id, initialState, delay))
  }

  object TestTrafficDetector {
    def apply()(implicit system: ActorSystem) = {
      TestActorRef(new TrafficDetector(""))
    }
  }

  object TestOnlyOneIsGreenSwitch {
    def apply(lights: Seq[LightState], timeout: FiniteDuration = testLightChangeDelay * 20)(implicit system: ActorSystem) = {
      val workers = lights zip (1 to lights.size) map { case (l, c) => (""+c -> TestTrafficLight(""+c, l, testLightChangeDelay)) }
      TestActorRef(new OnlyOneIsGreenSwitch(workers.toMap, timeout))
    }
  }

  object TestOnlyOneIsGreenSwitchFSM {
    def apply(lights: Seq[LightState], timeout: FiniteDuration = testLightChangeDelay * 20)(implicit system: ActorSystem) = {
      val workers = lights zip (1 to lights.size) map { case (l, c) => (""+c -> TestTrafficLightFSM(""+c, l, testLightChangeDelay)) }
      TestActorRef(new OnlyOneIsGreenSwitchFSM(workers.toMap, timeout))
    }
  }

  object TestTrafficDirector {
    def apply(lights: Seq[LightState], timeout: FiniteDuration = testLightChangeDelay * 20)(implicit system: ActorSystem) = {
      val lightManager = TestOnlyOneIsGreenSwitch(lights, timeout)
      val detectors = lights zip (1 to lights.size) map { case (l, c) => (TestTrafficDetector(), ""+c) }
      TestActorRef(new TrafficDirector(lightManager, Set(detectors: _*)))
    }
  }

  def stateOfTrafficLight(ref: ActorRef): LightState = ref.asInstanceOf[TestActorRef[TrafficLight]].underlyingActor.state
  def stateOfTrafficLightFSM(ref: ActorRef): LightState = ref.asInstanceOf[TestActorRef[TrafficLightFSM]].underlyingActor.stateName
}
