package trafficlightscontrol.actors

import akka.testkit._
import scala.concurrent.duration._
import scala.concurrent.{ Future, Await }
import akka.actor._
import akka.pattern.{ ask, pipe }

trait TrafficSystemTestKit {

  val testLightChangeDelay: FiniteDuration = 50.milliseconds

  def testSwitchStrategy(size: Int): SwitchStrategy = {
    var n = -1
    (currentGreenId: String, memberIds: Seq[String]) => {
      n = (n + 1) % size
      s"${n + 1}"
    }
  }

  object TestLight {
    def apply(id: String, initialState: LightState = RedLight, delay: FiniteDuration = testLightChangeDelay, automatic: Boolean = true)(implicit system: ActorSystem) =
      TestActorRef(new Light(id, initialState, delay, automatic))
  }

  object TestLightFSM {
    def apply(id: String, initialState: LightState = RedLight, delay: FiniteDuration = testLightChangeDelay, automatic: Boolean = true)(implicit system: ActorSystem) =
      TestActorRef(new LightFSM(id, initialState, delay, automatic))
  }

  object TestSwitch {
    def apply(id: String, lights: Seq[LightState], timeout: FiniteDuration = testLightChangeDelay * 20)(implicit system: ActorSystem) = {
      val workers = lights zip (1 to lights.size) map { case (l, c) => Light.props(""+c, l, testLightChangeDelay, true) }
      TestActorRef(new Switch(id, workers, timeout, testSwitchStrategy(lights.size)))
    }
  }

  object TestSwitchFSM {
    def apply(id: String, lights: Seq[LightState], timeout: FiniteDuration = testLightChangeDelay * 20)(implicit system: ActorSystem) = {
      val workers = lights zip (1 to lights.size) map { case (l, c) => LightFSM.props(""+c, l, testLightChangeDelay, true) }
      TestFSMRef(new SwitchFSM(id, workers, timeout, testSwitchStrategy(lights.size)))
    }
  }

  object TestGroup {
    def apply(id: String, lights: Seq[LightState], timeout: FiniteDuration = testLightChangeDelay * 20)(implicit system: ActorSystem) = {
      val workers = lights zip (1 to lights.size) map { case (l, c) => Light.props(""+c, l, testLightChangeDelay, true) }
      TestActorRef(new Group(id, workers, timeout))
    }
  }

  object TestGroupFSM {
    def apply(id: String, lights: Seq[LightState], timeout: FiniteDuration = testLightChangeDelay * 20)(implicit system: ActorSystem) = {
      val workers = lights zip (1 to lights.size) map { case (l, c) => LightFSM.props(""+c, l, testLightChangeDelay, true) }
      TestFSMRef(new GroupFSM(id, workers, timeout))
    }
  }

  def stateOfLightFSM(ref: ActorRef): LightState = ref.asInstanceOf[TestActorRef[LightFSM]].underlyingActor.stateName
}
