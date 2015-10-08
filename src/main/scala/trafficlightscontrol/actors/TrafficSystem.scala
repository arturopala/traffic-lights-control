package trafficlightscontrol.actors

import akka.actor._
import trafficlightscontrol.model._
import trafficlightscontrol.dsl._

object TrafficSystem {
  def props(component: Component)(implicit materializer: TrafficSystemMaterializer): Props = materializer.props(component)
}

trait TrafficSystemMaterializer {

  def lightProps(light: Light): Props
  def switchProps(switch: Switch): Props
  def groupProps(group: Group): Props

  def props(component: Component)(implicit materializer: TrafficSystemMaterializer): Props = component match {
    case light: Light   => lightProps(light)
    case switch: Switch => switchProps(switch)
    case group: Group   => groupProps(group)
  }
}

object TrafficSystemMaterializer extends TrafficSystemMaterializer {
  def lightProps(light: Light): Props = LightActor.props(light.id, light.initialState, light.configuration)
  def switchProps(switch: Switch): Props = {
    val memberProps = switch.members.map(props(_)(this))
    SwitchActor.props(switch.id, memberProps, switch.configuration, switch.strategy)
  }
  def groupProps(group: Group): Props = {
    val memberProps = group.members.map(props(_)(this))
    GroupActor.props(group.id, memberProps, group.configuration)
  }
}

object TrafficSystemMaterializerFSM extends TrafficSystemMaterializer {
  def lightProps(light: Light): Props = LightFSM.props(light.id, light.initialState, light.configuration)
  def switchProps(switch: Switch): Props = {
    val memberProps = switch.members.map(props(_)(this))
    SwitchFSM.props(switch.id, memberProps, switch.configuration, switch.strategy)
  }
  def groupProps(group: Group): Props = {
    val memberProps = group.members.map(props(_)(this))
    GroupFSM.props(group.id, memberProps, group.configuration)
  }
}