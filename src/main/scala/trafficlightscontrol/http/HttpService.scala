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
import akka.http.scaladsl.model.ws._
import akka.http.scaladsl.server._
import StatusCodes._
import Directives._

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._
import DefaultJsonProtocol._

import org.reactivestreams.Publisher

import trafficlightscontrol.actors._
import trafficlightscontrol.model._

/**
 * Http service exposing REST and WS API.
 */
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

  def getReport: Future[ReportEvent] = {
    monitoring.actor ? GetReportQuery map (_.asInstanceOf[ReportEvent])
  }

  def getStatusOpt(id: Id): Future[Option[StatusEvent]] = {
    monitoring.actor ? GetStatusQuery(id) map (_.asInstanceOf[Option[StatusEvent]])
  }

  def getStatusPublisher(predicate: Id => Boolean): Future[Publisher[StatusEvent]] = {
    monitoring.actor ? GetPublisherQuery(predicate) map (_.asInstanceOf[Publisher[StatusEvent]])
  }

  def getStatusPublisher: Future[Publisher[StatusEvent]] = {
    monitoring.actor ? GetPublisherQuery(_ => true) map (_.asInstanceOf[Publisher[StatusEvent]])
  }

  def handleWebsocket: Directive1[UpgradeToWebsocket] =
    optionalHeaderValueByType[UpgradeToWebsocket](()).flatMap {
      case Some(upgrade) ⇒ provide(upgrade)
      case None          ⇒ reject(ExpectedWebsocketRequestRejection)
    }

  def publisherAsMessageSource[A](p: Publisher[A])(f: A => String): Source[TextMessage, Unit] = Source(p).map(e => TextMessage.Strict(f(e)))

  val idPattern = "\\w{1,128}".r
  val statusEventToString: StatusEvent => String = e => s"${e.id}:${e.state.id}"
  val lightStateToString: StatusEvent => String = e => e.state.id
  val forAllIds: Id => Boolean = _ => true
  def onlyFor(id: Id): Id => Boolean = x => x == id

  val route = handleRejections(rejectionHandler) {
    handleExceptions(exceptionHandler) {
      get {
        pathPrefix("api" / "lights") {
          pathEnd {
            complete(getReport)
          } ~
            path(idPattern) { id =>
              onSuccess(getStatusOpt(id)) {
                case Some(status) => complete(status)
                case None         => complete(NotFound, s"Light #$id not found!")
              }
            }
        } ~
          pathPrefix("ws" / "lights") {
            pathEnd {
              handleWebsocket { websocket =>
                onSuccess(getStatusPublisher(forAllIds)) { p =>
                  complete(websocket.handleMessagesWithSinkSource(Sink.ignore, publisherAsMessageSource(p)(statusEventToString), None))
                }
              }
            } ~
              path(idPattern) { id =>
                handleWebsocket { websocket =>
                  onSuccess(getStatusPublisher(onlyFor(id))) { p =>
                    complete(websocket.handleMessagesWithSinkSource(Sink.ignore, publisherAsMessageSource(p)(lightStateToString), None))
                  }
                }
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
  }

}
