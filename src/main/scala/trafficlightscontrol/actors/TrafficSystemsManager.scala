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

  import scala.collection.mutable.Map

  val installedSystems: Map[Id, (Component, Option[ActorRef], SystemHistory)] = Map()

  def receive: Receive = {

    case InstallComponentCommand(component, system) =>
      installedSystems.get(system) match {
        case Some(_) =>
          sender ! InstallComponentFailedEvent(component, system, s"System with id=$system already installed!")
          log.error(s"Traffic system $system cannot be INSTALLED twice!")
        case _ =>
          installedSystems(system) = (component, None, SystemHistory().installed())
          sender ! InstallComponentSucceededEvent(component, system)
          log.info(s"Traffic system $system has been INSTALLED")
      }

    case command @ StartSystemCommand(system) => installedSystems.get(system) match {
      case Some((component, None, history)) =>
        val props = TrafficSystem.props(component, system)(TrafficSystemMaterializer)
        val trafficSystem = context.actorOf(props)
        context.watch(trafficSystem)
        installedSystems(system) = (component, Some(trafficSystem), history)
        trafficSystem ! command
      case Some((_, Some(_), _)) =>
        log.warning(s"Could not start system twice, $system alredy deployed!")
        sender ! SystemStartFailureEvent(system, s"Could not start, system $system already running!")
      case _ =>
        sender ! SystemStartFailureEvent(system, s"Could not start, system $system not yet installed!")
    }

    case SystemStartedEvent(system) => installedSystems.get(system) match {
      case Some((component, trafficSystemOpt, history)) =>
        installedSystems(system) = (component, trafficSystemOpt, history.started())
        log.info(s"Traffic system $system just STARTED")
      case _ =>
    }

    case command @ StopSystemCommand(system) => installedSystems.get(system) match {
      case Some((component, Some(trafficSystem), history)) =>
        trafficSystem ! command
        log.info(s"Traffic system $system about to STOP")
      case Some((_, None, _)) =>
        sender ! SystemStopFailureEvent(system, s"Could not stop, system $system not running!")
      case _ =>
        sender ! SystemStopFailureEvent(system, s"Could not stop, system $system not yet installed!")
    }

    case SystemInfoQuery(id) => installedSystems.get(id) match {
      case Some((component, _, history)) => sender ! SystemInfoEvent(id, component, component.configuration.interval, history)
      case _                             => sender ! CommandIgnoredEvent
    }

    case Terminated(trafficSystem) =>
      installedSystems find {
        case (system, (_, Some(actorRef), _)) => actorRef == trafficSystem
        case _                                => false
      } foreach {
        case (system, (component, _, history)) =>
          installedSystems(system) = (component, None, history.terminated())
          log.info(s"Traffic system $system is TERMINATED")
      }

    case CommandIgnoredEvent =>

  }

}
