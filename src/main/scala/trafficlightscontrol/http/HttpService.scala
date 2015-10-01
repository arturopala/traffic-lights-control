package trafficlightscontrol.http

import scala.concurrent.duration._
import scala.concurrent.{ Future }
import scala.util.{ Try, Success, Failure }
import scala.util.control.NonFatal

import akka.actor.{ Actor, ActorSystem, Props, ActorRef, ActorLogging }
import akka.pattern.ask
import akka.util.Timeout

import akka.stream._
import akka.stream.scaladsl._
import akka.http.scaladsl._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import StatusCodes._
import Directives._

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._
import DefaultJsonProtocol._

import trafficlightscontrol.actors._
import trafficlightscontrol.model._

class HttpService(monitoring: Monitoring)(implicit system: ActorSystem, materializer: ActorMaterializer) extends SprayJsonSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  import JsonProtocol._

  implicit val timeout = Timeout(5.seconds)

  def bind(host: String, port: Int): Future[Http.ServerBinding] = {
    Http().bindAndHandle(route, host, port) andThen {
      case Success(_) => println(s"Server online at http://$host:$port/")
      case Failure(e) => println(s"Error starting HTTP server: $e")
    }
  }

  val exceptionHandler = {
    ExceptionHandler {
      case e: Throwable =>
        complete(StatusCodes.InternalServerError)
    }
  }

  val rejectionHandler = {
    RejectionHandler.newBuilder()
      .handle {
        case MissingQueryParamRejection(param) =>
          complete(BadRequest, s"Request is missing required query parameter '$param'")
      }
      .handleAll[MethodRejection] { methodRejections ⇒
        val names = methodRejections.map(_.supported.name)
        complete(MethodNotAllowed, s"Can't do that! Supported: ${names mkString " or "}!")
      }
      .handleNotFound { complete(NotFound, "Not here!") }
      .result()
  }

  val route = handleRejections(rejectionHandler) {
    handleExceptions(exceptionHandler) {
      pathPrefix("lights") {
        pathEnd {
          get { complete { monitoring.actor ? GetReportQuery map (_.asInstanceOf[ReportEvent]) } }
        } ~
          path("\\w{1,128}".r) { id =>
            onSuccess(monitoring.actor ? GetStatusQuery(id) map (_.asInstanceOf[Option[StatusEvent]])) {
              case Some(status) => complete(status)
              case None         => complete(NotFound, s"Light #$id not found!")
            }
          }
      } ~
        pathPrefix("ws" / "lights") {
          get { complete("ws") }
        } ~
        pathPrefix("") {
          getFromResourceDirectory("")
        } ~
        path("") {
          getFromResource("index.html")
        }
    }
  }

}
