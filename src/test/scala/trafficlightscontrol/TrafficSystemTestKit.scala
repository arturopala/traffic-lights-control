package trafficlightscontrol

import akka.testkit.TestActorRef
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.actor.ActorRef

trait TrafficSystemTestKit {

  val testLightChangeDelay: FiniteDuration = 100 milliseconds

  object TestLight {
    def apply(id: String = "1", initialState: LightState = RedLight, delay: FiniteDuration = testLightChangeDelay, automatic: Boolean = true)(implicit system: ActorSystem) =
      TestActorRef(new Light(id, initialState, delay, automatic))
  }

  object TestLightFSM {
    def apply(id: String = "1", initialState: LightState = RedLight, delay: FiniteDuration = testLightChangeDelay, automatic: Boolean = true)(implicit system: ActorSystem) =
      TestActorRef(new LightFSM(id, initialState, delay, automatic))
  }

  object TestSwitch {
    def apply(lights: Seq[LightState], timeout: FiniteDuration = testLightChangeDelay * 20)(implicit system: ActorSystem) = {
      val workers = lights zip (1 to lights.size) map { case (l, c) => TestLight(""+c, l, testLightChangeDelay) }
      TestActorRef(new Switch(workers, timeout))
    }
  }

  object TestSwitchFSM {
    def apply(lights: Seq[LightState], timeout: FiniteDuration = testLightChangeDelay * 20)(implicit system: ActorSystem) = {
      val workers = lights zip (1 to lights.size) map { case (l, c) => (""+c -> TestLightFSM(""+c, l, testLightChangeDelay)) }
      TestActorRef(new SwitchFSM(workers.toMap, timeout))
    }
  }

  def stateOfLight(ref: ActorRef): LightState = ref.asInstanceOf[TestActorRef[Light]].underlyingActor.state
  def stateOfLightFSM(ref: ActorRef): LightState = ref.asInstanceOf[TestActorRef[LightFSM]].underlyingActor.stateName
}
