package trafficlightscontrol

import akka.actor.ActorRef

package object actors {

  implicit class ActorRefOption(actorRefOpt: Option[ActorRef]) {
    def !(msg: Any)(implicit sender: ActorRef): Unit = actorRefOpt.foreach(_.tell(msg, sender))
  }

  implicit class ActorRefColl(actorRefColl: Iterable[ActorRef]) {
    def !(msg: Any)(implicit sender: ActorRef): Unit = actorRefColl.foreach(_.tell(msg, sender))
  }

  implicit class ActorRefMap(actorRefMap: Map[String, ActorRef]) {
    def !(msg: Any)(implicit sender: ActorRef): Unit = actorRefMap.values.foreach(_.tell(msg, sender))
  }

}
