package trafficlightscontrol

import scala.concurrent.duration._
import trafficlightscontrol.model._

object dsl {

  sealed trait Component { def id: Id }
  sealed trait CompositeComponent extends Component { def members: Iterable[Component] }
  case class Light(id: Id, initialState: LightState, delay: FiniteDuration) extends Component
  case class Switch(id: Id, strategy: SwitchStrategy, timeout: FiniteDuration, members: Iterable[Component]) extends CompositeComponent
  case class Group(id: Id, timeout: FiniteDuration, members: Iterable[Component]) extends CompositeComponent

}

