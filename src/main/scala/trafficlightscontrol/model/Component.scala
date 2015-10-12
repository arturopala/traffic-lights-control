package trafficlightscontrol.model

sealed trait Component { def id: Id; def configuration: Configuration }
sealed trait CompositeComponent extends Component { def members: Iterable[Component] }

case class Light(id: Id, initialState: LightState = RedLight)(implicit val configuration: Configuration) extends Component
case class Switch(id: Id, strategy: SwitchStrategy, members: Component*)(implicit val configuration: Configuration) extends CompositeComponent
case class Group(id: Id, members: Component*)(implicit val configuration: Configuration) extends CompositeComponent

