package trafficlightscontrol

import akka.actor.{ ActorSystem, Actor, Props, ActorLogging, ActorRef, ActorRefFactory }
import akka.io.IO
import spray.can.Http
import spray.can.server.UHttp
import spray.can.websocket
import spray.can.websocket.frame.{ BinaryFrame, TextFrame }
import spray.http.HttpRequest
import spray.can.websocket.FrameCommandFailed

final case class Push(msg: String)

class WebSocketServiceActor(webSocketActor: ActorRef, httpActor: ActorRef) extends Actor with ActorLogging {
  def receive = {
    // when a new connection comes in we register a WebSocketConnection actor
    // as the per connection handler
    case Http.Connected(remoteAddress, localAddress) =>
      val serverConnection = sender()
      val conn = context.actorOf(Props(classOf[WebSocketWorker], serverConnection, webSocketActor, httpActor))
      serverConnection ! Http.Register(conn)
  }
}

class WebSocketWorker(val serverConnection: ActorRef, webSocketActor: ActorRef, httpActor: ActorRef) extends spray.routing.HttpServiceActor with websocket.WebSocketServerWorker {
  override def receive = handshaking orElse businessLogicNoUpgrade orElse closeLogic

  def businessLogic: Receive = {

    case msg @ (_: BinaryFrame | _: TextFrame) =>
      webSocketActor forward msg

    case Push(msg) => send(TextFrame(msg))

    case msg: FrameCommandFailed =>
      log.error("frame command failed", msg)

    case msg: HttpRequest => httpActor forward msg
  }

  def businessLogicNoUpgrade: Receive = {
    case msg => httpActor forward msg
  }
}

class WebServiceActor extends Actor {
  def receive = {
    case TextFrame(msg) => println(msg)
    case BinaryFrame(bytes) => println(bytes.mkString("[", ",", "]"))
  }
}
