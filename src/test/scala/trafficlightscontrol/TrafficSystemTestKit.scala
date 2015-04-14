package trafficlightscontrol

import akka.testkit.TestActorRef
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.actor.ActorRef

trait TrafficSystemTestKit {

  val testLightChangeDelay: FiniteDuration = 100 milliseconds

  object TestLight {
    def apply(id: String = "1", initialState: LightState = RedLight, delay: FiniteDuration = testLightChangeDelay)(implicit system: ActorSystem) =
      TestActorRef(new Light(id, initialState, delay))
  }

  object TestLightFSM {
    def apply(id: String = "1", initialState: LightState = RedLight, delay: FiniteDuration = testLightChangeDelay)(implicit system: ActorSystem) =
      TestActorRef(new LightFSM(id, initialState, delay))
  }

  object TestTrafficDetector {
    def apply()(implicit system: ActorSystem) = {
      TestActorRef(new TrafficDetector(""))
    }
  }

  object TestOnlyOneIsGreenSwitch {
    def apply(lights: Seq[LightState], timeout: FiniteDuration = testLightChangeDelay * 20)(implicit system: ActorSystem) = {
      val workers = lights zip (1 to lights.size) map { case (l, c) => (""+c -> TestLight(""+c, l, testLightChangeDelay)) }
      TestActorRef(new OnlyOneIsGreenSwitch(workers.toMap, timeout))
    }
  }

  object TestOnlyOneIsGreenSwitchFSM {
    def apply(lights: Seq[LightState], timeout: FiniteDuration = testLightChangeDelay * 20)(implicit system: ActorSystem) = {
      val workers = lights zip (1 to lights.size) map { case (l, c) => (""+c -> TestLightFSM(""+c, l, testLightChangeDelay)) }
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

  def stateOfLight(ref: ActorRef): LightState = ref.asInstanceOf[TestActorRef[Light]].underlyingActor.state
  def stateOfLightFSM(ref: ActorRef): LightState = ref.asInstanceOf[TestActorRef[LightFSM]].underlyingActor.stateName
}
