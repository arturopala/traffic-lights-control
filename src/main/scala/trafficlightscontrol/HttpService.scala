package trafficlightscontrol

import akka.actor.{ Actor, ActorSystem, Props, ActorRef, ActorLogging }
import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import spray.routing._
import spray.http._
import MediaTypes._

class HttpServiceActor(monitoring: Monitoring) extends Actor with TrafficHttpService {
  def actorRefFactory = context
  val route = createRoutes(monitoring)
  def receive = runRoute(route)
}

trait TrafficHttpService extends HttpService {

  import Directives._

  object GetReportQueryTimeout

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

  def createRoutes(monitoring: Monitoring) =
    handleRejections(rejectionHandler) {
      handleExceptions(exceptionHandler) {
        path("status") {
          get {
            streamStatusResponse(monitoring)
          }
        } ~
          pathPrefix("") {
            getFromResourceDirectory("")
          } ~
          path("") {
            getFromResource("index.html")
          }
      }
    }

  def streamStatusResponse(monitoring: Monitoring)(ctx: RequestContext): Unit =
    actorRefFactory.actorOf {
      Props {
        new Actor with ActorLogging {

          val responseStart = HttpResponse(entity = HttpEntity(`application/json`, "{\"status\":{"))
          ctx.responder ! ChunkedResponseStart(responseStart)

          def receive = {
            case ReportEvent(report) => {
              timeoutTask.cancel()
              ctx.responder ! MessageChunk(report map { case (k, v) => s""""$k":"$v"""" } mkString ("", ",", "}}"))
              ctx.responder ! ChunkedMessageEnd
              context.stop(self)
            }
            case GetReportQueryTimeout => {
              ctx.responder ! MessageChunk("}}")
              ctx.responder ! ChunkedMessageEnd
              context.stop(self)
            }
            case ev: Http.ConnectionClosed => {
              timeoutTask.cancel()
              log.warning("Stopping response streaming due to {}", ev)
            }

          }

          val timeoutTask = context.system.scheduler.scheduleOnce(1.seconds, self, GetReportQueryTimeout)(context.system.dispatcher)

          monitoring.report

        }
      }
    }
}
