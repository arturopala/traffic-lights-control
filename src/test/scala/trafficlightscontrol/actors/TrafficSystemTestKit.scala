package trafficlightscontrol.actors

import akka.testkit._
import scala.concurrent.duration._
import scala.concurrent.{ Future, Await }
import akka.actor._
import akka.pattern.{ ask, pipe }

import trafficlightscontrol.model._

trait TrafficSystemTestKit {

  val config = Configuration(interval = 1.second, delayGreenToRed = 60.milliseconds, delayRedToGreen = 40.milliseconds, sequenceDelay = 10.milliseconds, timeout = 1.second)

  def testSequenceStrategy(size: Int): SequenceStrategy = new SequenceStrategy {
    val name = "TestSequenceStrategy"
    var n = -1
    def apply(current: Id, members: scala.collection.Seq[Id]): Id = {
      n = (n + 1) % size
      s"${n + 1}"
    }
  }

  object TestLight {
    def apply(id: String, initialState: LightState = RedLight, configuration: Configuration = config)(implicit system: ActorSystem) =
      TestActorRef(new LightActor(id, initialState, configuration))
  }

  object TestLightFSM {
    def apply(id: String, initialState: LightState = RedLight, configuration: Configuration = config)(implicit system: ActorSystem) =
      TestActorRef(new LightFSM(id, initialState, configuration))
  }

  object TestSequence {
    def apply(id: String, lights: Seq[LightState], configuration: Configuration = config)(implicit system: ActorSystem) = {
      val workers = lights zip (1 to lights.size) map { case (l, c) => LightActor.props(""+c, l, configuration) }
      TestActorRef(new SequenceActor(id, workers, configuration, testSequenceStrategy(lights.size)))
    }
  }

  object TestSequenceFSM {
    def apply(id: String, lights: Seq[LightState], configuration: Configuration = config)(implicit system: ActorSystem) = {
      val workers = lights zip (1 to lights.size) map { case (l, c) => LightFSM.props(""+c, l, configuration) }
      TestFSMRef(new SequenceFSM(id, workers, configuration, testSequenceStrategy(lights.size)))
    }
  }

  object TestGroup {
    def apply(id: String, lights: Seq[LightState], configuration: Configuration = config)(implicit system: ActorSystem) = {
      val workers = lights zip (1 to lights.size) map { case (l, c) => LightActor.props(""+c, l, configuration) }
      TestActorRef(new GroupActor(id, workers, configuration))
    }
  }

  object TestGroupFSM {
    def apply(id: String, lights: Seq[LightState], configuration: Configuration = config)(implicit system: ActorSystem) = {
      val workers = lights zip (1 to lights.size) map { case (l, c) => LightFSM.props(""+c, l, configuration) }
      TestFSMRef(new GroupFSM(id, workers, configuration))
    }
  }

  def stateOfLightFSM(ref: ActorRef): LightState = ref.asInstanceOf[TestActorRef[LightFSM]].underlyingActor.stateName

}
