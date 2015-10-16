package trafficlightscontrol.model

import scala.concurrent.duration._

sealed trait Component { def id: Id; def configuration: Configuration }
sealed trait CompositeComponent extends Component { def members: Iterable[Component] }
sealed trait SingleMemberComponent extends CompositeComponent { def member: Component; def members: Iterable[Component] = Seq(member) }

case class Light(id: Id, initialState: LightState = RedLight)(implicit val configuration: Configuration) extends Component
case class Sequence(id: Id, strategy: SequenceStrategy, members: Component*)(implicit val configuration: Configuration) extends CompositeComponent
case class Group(id: Id, members: Component*)(implicit val configuration: Configuration) extends CompositeComponent
case class Switch(id: Id, member: Component, initiallyGreen: Boolean = false, skipTicks: Int = 0)(implicit val configuration: Configuration) extends SingleMemberComponent
case class Pulse(id: Id, member: Component, skipTicks: Int = 0)(implicit val configuration: Configuration) extends SingleMemberComponent
case class Offset(id: Id, offset: FiniteDuration, member: Component)(implicit val configuration: Configuration) extends SingleMemberComponent

