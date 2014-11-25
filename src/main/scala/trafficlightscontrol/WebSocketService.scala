package trafficlightscontrol

import akka.actor.{ ActorSystem, Actor, Props, ActorLogging, ActorRef, ActorRefFactory }
import akka.io.IO
import spray.can.Http
import spray.can.server.UHttp
import spray.can.websocket
import spray.can.websocket.frame.{ BinaryFrame, TextFrame }
import spray.http.HttpRequest
import spray.can.websocket.FrameCommandFailed
import spray.http.Uri
import spray.can.websocket.`package`.FrameCommand
import spray.http.Uri.Path

final case class WsPush(msg: String)
final case class WsPull(path: String, msg: String)

class WebSocketServiceActor(webSocketListenerActor: ActorRef, httpListenerActor: ActorRef) extends Actor with ActorLogging {
  def receive = {
    // when a new connection comes in we register a WebSocketConnection actor
    // as the per connection handler
    case Http.Connected(remoteAddress, localAddress) =>
      val serverConnection = sender()
      val conn = context.actorOf(Props(classOf[WebSocketServiceWorker], serverConnection, webSocketListenerActor, httpListenerActor))
      serverConnection ! Http.Register(conn)
  }
}

class WebSocketServiceWorker(val serverConnection: ActorRef, webSocketListenerActor: ActorRef, httpListenerActor: ActorRef) extends spray.routing.HttpServiceActor with websocket.WebSocketServerWorker {
  override def receive = preHandshaking orElse handshaking orElse businessLogicNoUpgrade orElse closeLogic

  var path: String = "???"

  def preHandshaking: Receive = {

    case msg @ websocket.HandshakeRequest(state) =>
      state match {
        case wsFailure: websocket.HandshakeFailure => handshaking(msg)
        case wsContext: websocket.HandshakeContext =>
          path = wsContext.request.uri.path.toString
          handshaking(msg)
      }
  }

  def businessLogic: Receive = {

    case TextFrame(bytes) =>
      webSocketListenerActor ! WsPull(path, bytes.decodeString("utf-8"))

    case BinaryFrame(bytes) =>
      webSocketListenerActor ! WsPull(path, bytes.decodeString("utf-8"))

    case WsPush(msg) => send(TextFrame(msg))

    case msg: FrameCommandFailed =>
      log.error("frame command failed", msg)

    case msg: HttpRequest => httpListenerActor forward msg
  }

  def businessLogicNoUpgrade: Receive = {
    case msg => httpListenerActor forward msg
  }

}

class WebServiceActor extends Actor {
  def receive = {
    case WsPull(path, text) =>
      sender ! WsPush(s"$path: $text")
  }
}
