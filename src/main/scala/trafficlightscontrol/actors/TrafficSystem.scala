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
  def sequenceProps(sequence: Sequence, systemId: Id): Props
  def groupProps(group: Group, systemId: Id): Props

  def props(component: Component, systemId: Id)(implicit materializer: TrafficSystemMaterializer): Props = component match {
    case light: Light       => lightProps(light, systemId)
    case sequence: Sequence => sequenceProps(sequence, systemId)
    case group: Group       => groupProps(group, systemId)
  }
}

object TrafficSystemMaterializer extends TrafficSystemMaterializer {
  def lightProps(light: Light, systemId: Id): Props = LightActor.props(combineId(systemId, light.id), light.initialState, light.configuration)
  def sequenceProps(sequence: Sequence, systemId: Id): Props = {
    val memberProps = sequence.members.map(props(_, systemId)(this))
    SequenceActor.props(combineId(systemId, sequence.id), memberProps, sequence.configuration, sequence.strategy)
  }
  def groupProps(group: Group, systemId: Id): Props = {
    val memberProps = group.members.map(props(_, systemId)(this))
    GroupActor.props(combineId(systemId, group.id), memberProps, group.configuration)
  }
}

object TrafficSystemMaterializerFSM extends TrafficSystemMaterializer {
  def lightProps(light: Light, systemId: Id): Props = LightFSM.props(combineId(systemId, light.id), light.initialState, light.configuration)
  def sequenceProps(sequence: Sequence, systemId: Id): Props = {
    val memberProps = sequence.members.map(props(_, systemId)(this))
    SequenceFSM.props(combineId(systemId, sequence.id), memberProps, sequence.configuration, sequence.strategy)
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

  var running: Boolean = false

  def receive: Receive = {

    case StartSystemCommand(id) =>
      if (id == systemId && !running) {
        clock = context.system.scheduler.schedule(component.configuration.delayRedToGreen, component.configuration.interval, componentRef, TickEvent)(context.system.dispatcher, self)
        running = true
        sender ! SystemStartedEvent(systemId)
      }
      else sender ! CommandIgnoredEvent

    case StopSystemCommand(id) =>
      if (id == systemId && running) {
        clock.cancel()
        context.stop(self)
      }
      else sender ! CommandIgnoredEvent

    case message => componentRef ! message
  }

}