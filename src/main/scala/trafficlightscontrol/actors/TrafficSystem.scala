package trafficlightscontrol.actors

import akka.actor._
import trafficlightscontrol.model._

object TrafficSystem {
  def props(component: Component, systemId: Id)(implicit materializer: TrafficSystemMaterializer): Props = {
    val componentProps = materializer.props(component, systemId)
    TrafficSystemActor.props(componentProps, component, systemId)
  }
  def combineId(systemId: Id, id: Id): Id = systemId+"_"+id
}

import TrafficSystem.combineId

trait TrafficSystemMaterializer {

  def lightProps(light: Light, systemId: Id): Props
  def switchProps(switch: Switch, systemId: Id): Props
  def groupProps(group: Group, systemId: Id): Props

  def props(component: Component, systemId: Id)(implicit materializer: TrafficSystemMaterializer): Props = component match {
    case light: Light   => lightProps(light, systemId)
    case switch: Switch => switchProps(switch, systemId)
    case group: Group   => groupProps(group, systemId)
  }
}

object TrafficSystemMaterializer extends TrafficSystemMaterializer {
  def lightProps(light: Light, systemId: Id): Props = LightActor.props(combineId(systemId, light.id), light.initialState, light.configuration)
  def switchProps(switch: Switch, systemId: Id): Props = {
    val memberProps = switch.members.map(props(_, systemId)(this))
    SwitchActor.props(combineId(systemId, switch.id), memberProps, switch.configuration, switch.strategy)
  }
  def groupProps(group: Group, systemId: Id): Props = {
    val memberProps = group.members.map(props(_, systemId)(this))
    GroupActor.props(combineId(systemId, group.id), memberProps, group.configuration)
  }
}

object TrafficSystemMaterializerFSM extends TrafficSystemMaterializer {
  def lightProps(light: Light, systemId: Id): Props = LightFSM.props(combineId(systemId, light.id), light.initialState, light.configuration)
  def switchProps(switch: Switch, systemId: Id): Props = {
    val memberProps = switch.members.map(props(_, systemId)(this))
    SwitchFSM.props(combineId(systemId, switch.id), memberProps, switch.configuration, switch.strategy)
  }
  def groupProps(group: Group, systemId: Id): Props = {
    val memberProps = group.members.map(props(_, systemId)(this))
    GroupFSM.props(combineId(systemId, group.id), memberProps, group.configuration)
  }
}

object TrafficSystemActor {
  def props(componentProps: Props, component: Component, systemId: Id): Props = Props(classOf[TrafficSystemActor], componentProps, component, systemId)
}

class TrafficSystemActor(componentProps: Props, component: Component, systemId: Id) extends Actor with ActorLogging {

  val componentRef = context.actorOf(componentProps)
  var clock: Cancellable = _

  def receive: Receive = {
    case TickCommand =>
      componentRef ! ChangeToGreenCommand
    case StartSystemCommand(_) =>
      clock = context.system.scheduler.schedule(component.configuration.delayRedToGreen, component.configuration.interval, self, TickCommand)(context.system.dispatcher, self)
      log.info(s"Traffic system $systemId just STARTED")
    case StopSystemCommand(_) =>
      clock.cancel()
      context.stop(self)
    case message => componentRef ! message
  }

}