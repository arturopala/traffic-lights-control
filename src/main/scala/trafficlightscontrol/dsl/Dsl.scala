package trafficlightscontrol

import scala.concurrent.duration._
import trafficlightscontrol.model._

object dsl {

  sealed trait Component { def id: Id }
  sealed trait CompositeComponent extends Component { def members: Iterable[Component] }
  case class Light(id: Id, initialState: LightState, configuration: Configuration) extends Component
  case class Switch(id: Id, strategy: SwitchStrategy, configuration: Configuration, members: Iterable[Component]) extends CompositeComponent
  case class Group(id: Id, configuration: Configuration, members: Iterable[Component]) extends CompositeComponent

}

