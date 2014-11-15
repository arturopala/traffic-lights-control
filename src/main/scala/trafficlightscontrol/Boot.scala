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
import akka.actor.ActorLogging

object Boot extends App {
  implicit val system = ActorSystem("app")
  val trafficSystem = system.actorOf(Props(classOf[TrafficSystem]), "traffic")
  val service = system.actorOf(Props(classOf[HttpServiceActor], trafficSystem), "httpservice")
  implicit val timeout = Timeout(5.seconds)
  IO(Http) ? Http.Bind(service, interface = "0.0.0.0", port = 8080)
}

class HttpServiceActor(trafficSystem: ActorRef) extends Actor with TrafficHttpService {
  def actorRefFactory = context
  val route = createRoutes(trafficSystem)
  def receive = runRoute(route)
}

trait TrafficHttpService extends HttpService {

  import Directives._
  implicit def executionContext = actorRefFactory.dispatcher

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

  def createRoutes(trafficSystem: ActorRef) =
    handleRejections(rejectionHandler) {
      handleExceptions(exceptionHandler) {
        path("status") {
          get {
            respondWithMediaType(`text/plain`) {
              streamStatusResponse(trafficSystem)
            }
          }
        }
      }
    }

  object Started

  def streamStatusResponse(trafficSystem: ActorRef)(ctx: RequestContext): Unit =
    actorRefFactory.actorOf {
      Props {
        new Actor with ActorLogging {

          trafficSystem ! GetStatusQuery

          val responseStart = HttpResponse(entity = HttpEntity(`text/plain`, ""))
          ctx.responder ! ChunkedResponseStart(responseStart).withAck(Started)

          def receive = {
            case Started =>
              ctx.responder ! MessageChunk("OK")
              ctx.responder ! ChunkedMessageEnd
              context.stop(self)

            /*case Ok(remaining) =>
              in(500.millis) {
                val nextChunk = MessageChunk("<li>" + DateTime.now.toIsoDateTimeString + "</li>")
                ctx.responder ! nextChunk.withAck(Sent)
              }*/

            case ev: Http.ConnectionClosed =>
              log.warning("Stopping response streaming due to {}", ev)
          }
        }
      }
    }
}
