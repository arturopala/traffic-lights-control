package trafficlightscontrol

import akka.actor.{ ActorSystem, Props, ActorRef }
import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import akka.actor.Actor
import spray.routing._
import spray.http._
import MediaTypes._
import akka.actor.ActorLogging

object Boot extends App {

  implicit val system = ActorSystem("app")

  val traffic: ActorRef = system.actorOf(Props(classOf[TrafficSystem]), "traffic")
  val monitoring: ActorRef = system.actorOf(Props(classOf[MonitoringActor], traffic), "monitoring")
  val service: ActorRef = system.actorOf(Props(classOf[HttpServiceActor], monitoring), "httpservice")

  implicit val timeout = Timeout(5.seconds)
  IO(Http) ? Http.Bind(service, interface = "0.0.0.0", port = 8080)
}

class HttpServiceActor(monitoring: ActorRef) extends Actor with TrafficHttpService {
  def actorRefFactory = context
  val route = createRoutes(monitoring)
  def receive = runRoute(route)
}

trait TrafficHttpService extends HttpService {

  import Directives._

  object GetStatusQueryTimeout

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

  def createRoutes(monitoring: ActorRef) =
    handleRejections(rejectionHandler) {
      handleExceptions(exceptionHandler) {
        path("status") {
          get {
            streamStatusResponse(monitoring)
          }
        } ~
          pathPrefix("") {
            getFromResourceDirectory("")
          }
      }
    }

  def streamStatusResponse(monitoring: ActorRef)(ctx: RequestContext): Unit =
    actorRefFactory.actorOf {
      Props {
        new Actor with ActorLogging {

          monitoring ! GetReportQuery

          val responseStart = HttpResponse(entity = HttpEntity(`application/json`, "{status:{"))
          ctx.responder ! ChunkedResponseStart(responseStart)

          def receive = {
            case ReportEvent(report) => {
              ctx.responder ! MessageChunk(report map { case (k, v) => s""""$k":"$v"""" } mkString ("", ",", "}}"))
              ctx.responder ! ChunkedMessageEnd
              context.stop(self)
            }
            case GetStatusQueryTimeout => {
              ctx.responder ! MessageChunk("}}")
              ctx.responder ! ChunkedMessageEnd
              context.stop(self)
            }
            case ev: Http.ConnectionClosed =>
              log.warning("Stopping response streaming due to {}", ev)
          }

          context.system.scheduler.scheduleOnce(1.seconds, self, GetStatusQueryTimeout)(context.system.dispatcher)

        }
      }
    }
}
