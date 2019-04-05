package trafficlightscontrol.http

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.pattern.ask
import akka.stream._
import akka.stream.scaladsl._
import akka.util.Timeout
import org.reactivestreams.Publisher
import trafficlightscontrol.actors._
import trafficlightscontrol.model._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Http service exposing REST and WS API.
  */
class HttpService(monitoring: Monitoring, manager: TrafficSystemsManager)(
  implicit system: ActorSystem,
  materializer: ActorMaterializer)
    extends SprayJsonSupport {

  import JsonProtocol._

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val timeout = Timeout(5.seconds)

  def bind(host: String, port: Int): Future[Http.ServerBinding] =
    Http().bindAndHandle(route, host, port) andThen {
      case Success(_) => println(s"Server online at http://$host:$port/")
      case Failure(e) => println(s"Error starting HTTP server: $e")
    }

  val exceptionHandler = {
    ExceptionHandler {
      case e: Throwable =>
        complete(StatusCodes.InternalServerError)
    }
  }

  val rejectionHandler = {
    RejectionHandler
      .newBuilder()
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

  def getFullReport: Future[ReportEvent] =
    monitoring.actor ? GetReportQuery map (_.asInstanceOf[ReportEvent])

  def getReport(systemId: Id): Future[ReportEvent] =
    monitoring.actor ? GetReportQuery(systemId) map (_.asInstanceOf[ReportEvent])

  def getStatusOpt(id: Id): Future[Option[StateChangedEvent]] =
    monitoring.actor ? GetStatusQuery(id) map (_.asInstanceOf[Option[StateChangedEvent]])

  def getStatusPublisher(predicate: Id => Boolean): Future[Publisher[StateChangedEvent]] =
    monitoring.actor ? GetPublisherQuery(predicate) map (_.asInstanceOf[Publisher[StateChangedEvent]])

  def getStatusPublisher: Future[Publisher[StateChangedEvent]] =
    monitoring.actor ? GetPublisherQuery(_ => true) map (_.asInstanceOf[Publisher[StateChangedEvent]])

  def getLayoutList: Future[Iterable[String]] =
    manager.actor ? GetSystemListQuery map (_.asInstanceOf[Iterable[String]])

  def getLayout(id: Id): Future[Component] =
    manager.actor ? GetSystemInfoQuery(id) map (_.asInstanceOf[SystemInfoEvent].component)

  import akka.http.scaladsl.model.ws.UpgradeToWebSocket

  def handleWebsocket: Directive1[UpgradeToWebSocket] =
    optionalHeaderValueByType[UpgradeToWebSocket](()).flatMap {
      case Some(upgrade) ⇒ provide(upgrade)
      case None ⇒ reject(ExpectedWebSocketRequestRejection)
    }

  def publisherAsMessageSource[A](p: Publisher[A])(f: A => String): Source[TextMessage, NotUsed] =
    Source.fromPublisher(p).map((e: A) => TextMessage(f(e))).named("tms")

  val idPattern = "\\w{1,128}".r
  val statusEventToString: StateChangedEvent => String = e => e.id + ":" + e.state.id
  val lightStateToString: StateChangedEvent => String = e => e.state.id
  val forAllIds: Id => Boolean = _ => true
  def forSystem(systemId: Id): Id => Boolean = x => x.startsWith(systemId)
  def forLight(id: Id): Id => Boolean = x => x == id

  val pathEmpty: Directive0 = pathEnd | pathSingleSlash

  val AccessControlAllowAll = RawHeader("Access-Control-Allow-Origin", "*")

  //////////////////////////////////////////////////////////////////////
  //                   Main routing configuration                     //
  //////////////////////////////////////////////////////////////////////
  val route = handleRejections(rejectionHandler) {
    handleExceptions(exceptionHandler) {
      logRequestResult("http", Logging.InfoLevel) {
        get {
          path("app.js") { getFromResource("public/app.js") } ~
            path("style.css") { getFromResource("public/style.css") } ~
            pathPrefix("api") {
              respondWithHeaders(AccessControlAllowAll) {
                pathPrefix("lights") {
                  pathEmpty {
                    complete(getFullReport)
                  } ~
                    pathPrefix(idPattern) { systemId =>
                      pathEmpty {
                        complete(getReport(systemId))
                      } ~
                        path(idPattern) { lightId =>
                          onSuccess(getStatusOpt(systemId + "_" + lightId)) {
                            case Some(status) => complete(status)
                            case None         => complete(NotFound)
                          }
                        }
                    }
                } ~
                  pathPrefix("layouts") {
                    pathEmpty {
                      onSuccess(getLayoutList) { layouts =>
                        complete(layouts)
                      }
                    } ~
                      path(idPattern) { systemId =>
                        onSuccess(getLayout(systemId)) { layout =>
                          complete(layout)
                        }
                      }
                  }
              }
            } ~
            pathPrefix("ws" / "lights") {
              pathEmpty {
                handleWebsocket { websocket =>
                  onSuccess(getStatusPublisher(forAllIds)) { p =>
                    complete(
                      websocket.handleMessagesWithSinkSource(
                        Sink.ignore,
                        publisherAsMessageSource(p)(statusEventToString),
                        None))
                  }
                }
              } ~
                pathPrefix(idPattern) { systemId =>
                  pathEmpty {
                    handleWebsocket { websocket =>
                      onSuccess(getStatusPublisher(forSystem(systemId))) { p =>
                        complete(
                          websocket.handleMessagesWithSinkSource(
                            Sink.ignore,
                            publisherAsMessageSource(p)(statusEventToString),
                            None))
                      }
                    }
                  } ~
                    path(idPattern) { lightId =>
                      handleWebsocket { websocket =>
                        onSuccess(getStatusPublisher(forLight(systemId + "_" + lightId))) { p =>
                          complete(
                            websocket.handleMessagesWithSinkSource(
                              Sink.ignore,
                              publisherAsMessageSource(p)(statusEventToString),
                              None))
                        }
                      }
                    }
                }
            } ~
            pathPrefix("assets") {
              getFromResourceDirectory("public/")
            } ~
            getFromResource("public/index.html")
        }
      }
    }
  }

}
