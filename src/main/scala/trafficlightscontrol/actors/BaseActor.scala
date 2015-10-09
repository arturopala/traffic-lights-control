package trafficlightscontrol.actors

import scala.concurrent.duration._
import akka.actor._
import trafficlightscontrol.model._

/**
 * Base trait of leaf-like traffic system components
 */
trait BaseLeafActor extends Actor with ActorLogging {

  def id: Id
  def configuration: Configuration

  def recipient: Option[ActorRef] = _recipient

  private var _recipient: Option[ActorRef] = None

  private val receiveRecipient: Receive = {
    case RegisterRecipientCommand(newRecipient) =>
      if (_recipient.isEmpty) {
        _recipient = Option(newRecipient)
        _recipient ! RecipientRegisteredEvent(id)
      }
  }

  val receiveUnhandled: Receive = {
    case TickEvent => ()
    case other =>
      log.error(s"Component ${this.id}: command not recognized $other")
  }

  private var delayTask: Cancellable = _

  def scheduleDelay(delay: FiniteDuration): Unit = {
    if (configuration.automatic) delayTask = context.system.scheduler.scheduleOnce(delay, self, CanContinueAfterDelayEvent)(context.system.dispatcher)
  }

  def cancelDelay(): Unit = {
    delayTask.cancel()
  }

  val receiveCommonLeafMessages: Receive = receiveRecipient orElse receiveUnhandled

}

/**
 * Base trait of node-like traffic system components (composing some members)
 */
trait BaseNodeActor extends BaseLeafActor {

  def receiveWhenIdle: Receive
  def memberProps: Iterable[Props]

  def members: Map[Id, ActorRef] = _members
  def memberIds: Seq[Id] = _memberIds

  private var _members: Map[Id, ActorRef] = Map()
  private var _memberIds: Seq[Id] = _

  override def preStart = {
    for (prop <- memberProps) {
      val member = context.actorOf(prop)
      member ! RegisterRecipientCommand(self)
    }
  }

  private var timeoutTask: Cancellable = _

  def scheduleTimeout(timeout: FiniteDuration): Unit = {
    timeoutTask = context.system.scheduler.scheduleOnce(timeout, self, TimeoutEvent)(context.system.dispatcher)
  }

  def cancelTimeout(): Unit = {
    timeoutTask.cancel()
  }

  /////////////////////////////////////////////////////////////////
  // STATE 0: INITIALIZING, WAITING FOR ALL MEMBERS REGISTRATION //
  /////////////////////////////////////////////////////////////////
  val receiveWhenInitializing: Receive = {
    case RecipientRegisteredEvent(id) =>
      _members = _members + (id -> sender())
      _memberIds = _members.keys.toSeq
      log.debug(s"Node ${this.id}: new member registered $id")
      if (_members.size == memberProps.size) {
        log.info(s"Node ${this.id} initialized. Members: ${memberIds.mkString(",")}")
        context.become(receiveWhenIdle orElse receiveCommonNodeMessages)
      }
  }

  private val receiveTick: Receive = {
    case TickEvent => members ! TickEvent
  }

  val receiveCommonNodeMessages: Receive = receiveTick orElse receiveCommonLeafMessages

}

