import akka.actor.ActorRef

package object trafficlightscontrol {

  type LightId = String

  implicit class ActorRefOption(actorRefOpt: Option[ActorRef]) {
    def !(msg: Any)(implicit sender: ActorRef): Unit = actorRefOpt.foreach(_.tell(msg, sender))
  }

}
