package trafficlightscontrol.actors

import akka.actor._
import akka.pattern.ask
import scala.collection._
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.mutable.{ Set, Map }

import trafficlightscontrol.model._

object TimeOffsetActor {
  def props(
    id:            Id,
    memberProp:    Props,
    configuration: Configuration,
    offset:        FiniteDuration
  ): Props =
    Props(classOf[TimeOffsetActor], id, memberProp, configuration, offset)
}

/**
 * TimeOffset is a component delaying messages by an time offset
 * @param offset how long to delay a message
 */
class TimeOffsetActor(
    val id:            Id,
    val memberProp:    Props,
    val configuration: Configuration,
    offset:            FiniteDuration
) extends SingleNodeActor {

  val receiveWhenIdle: Receive = {
    case msg => context.system.scheduler.scheduleOnce(offset, members.head._2, msg)(context.system.dispatcher)
  }

}
