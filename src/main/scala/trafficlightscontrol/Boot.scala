package trafficlightscontrol

import akka.actor.{ ActorSystem, Props }
import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

import akka.actor.Actor
import spray.routing._
import spray.http._
import MediaTypes._
import akka.actor.ActorRef

object Boot extends App {
  implicit val system = ActorSystem("app")
  val trafficSystem = system.actorOf(Props(classOf[TrafficSystem]), "traffic")
  val service = system.actorOf(Props(classOf[HttpServiceActor], trafficSystem), "httpservice")
  implicit val timeout = Timeout(5.seconds)
  IO(Http) ? Http.Bind(service, interface = "0.0.0.0", port = 8080)
}

class HttpServiceActor(trafficSystem: ActorRef) extends Actor with HttpService {
  def actorRefFactory = context
  val route = Routes.route(trafficSystem)
  def receive = runRoute(route)
}

object Routes {

  import Directives._

  val exceptionHandler = {
    ExceptionHandler {
      case e: Throwable =>
        complete(StatusCodes.InternalServerError)
    }
  }

  val rejectionHandler = {
    RejectionHandler {
      case MissingQueryParamRejection(param) :: _ =>
        complete(StatusCodes.BadRequest, s"Request is missing required query parameter '$param'")
    }
  }

  def route(trafficSystem: ActorRef) =
    handleRejections(rejectionHandler) {
      handleExceptions(exceptionHandler) {
        path("status") {
          get {
            respondWithMediaType(`text/plain`) {
              complete {
                trafficSystem ! GetStatusQuery
                "OK"
              }
            }
          }
        }
      }
    }
}
