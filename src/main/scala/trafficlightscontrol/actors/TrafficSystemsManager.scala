package trafficlightscontrol.actors

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.{ Props, Terminated }
import scala.concurrent.duration._
import akka.actor.ActorLogging
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._

import trafficlightscontrol.model._
import trafficlightscontrol.actors._

object TrafficSystemsManager {
  def props(): Props = Props(classOf[TrafficSystemsManager])
}

class TrafficSystemsManager() extends Actor with ActorLogging {

  var installedSystems: Map[Id, (Component, ActorRef, SystemHistory)] = Map()

  def receive: Receive = {

    case InstallComponentCommand(component, system) =>
      installedSystems.get(system) match {
        case Some(_) =>
          sender ! InstallComponentFailedEvent(component, system, s"System with id=$system already deployed!")
          log.error(s"Traffic system $system cannot be INSTALLED twice!")
        case None =>
          val props = TrafficSystem.props(component, system)(TrafficSystemMaterializer)
          val trafficSystem = context.actorOf(props)
          context.watch(trafficSystem)
          val startTime = System.currentTimeMillis()
          installedSystems += (system -> (component, trafficSystem, SystemHistory().installed()))
          sender ! InstallComponentSucceededEvent(component, system)
          log.info(s"Traffic system $system has been DEPLOYED")
      }

    case command @ StartSystemCommand(system) => installedSystems.get(system) match {
      case Some((component, trafficSystem, history)) =>
        trafficSystem ! command
        installedSystems += (system -> (component, trafficSystem, history.started()))
      case None =>
        sender ! SystemStartFailureEvent(system, s"Could not start, system $system not yet deployed!")
    }

    case command @ StopSystemCommand(system) => installedSystems.get(system) match {
      case Some((component, trafficSystem, history)) =>
        trafficSystem ! command
        installedSystems += (system -> (component, trafficSystem, history.stopped()))
      case None =>
        sender ! SystemStartFailureEvent(system, s"Could not stop, system $system not yet deployed!")
    }

    case SystemInfoQuery(id) => installedSystems.get(id) match {
      case Some((component, _, history)) => sender ! SystemInfoEvent(id, component, component.configuration.interval, history)
      case None                          => sender ! CommandIgnoredEvent
    }

    case Terminated(trafficSystem) =>
      installedSystems find {
        case (system, (_, actorRef, _)) => actorRef == trafficSystem
      } foreach {
        case (system, (component, actorRef, history)) =>
          installedSystems += (system -> (component, trafficSystem, history.terminated()))
          log.info(s"Traffic system $system is TERMINATED")
      }

  }

}
