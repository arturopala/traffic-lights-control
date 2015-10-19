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

  final def recipient: Option[ActorRef] = _recipient

  private var _recipient: Option[ActorRef] = None

  private val receiveRecipient: Receive = {
    case RegisterRecipientCommand(newRecipient) =>
      if (_recipient.isEmpty) {
        _recipient = Option(newRecipient)
        _recipient ! RecipientRegisteredEvent(id)
      }
  }

  private val receiveUnhandled: Receive = {
    case event: Event => ()
    case command: Command =>
      recipient ! MessageIgnoredEvent(command)
    case other =>
      log.debug(s"Component ${this.id}: message not recognized $other")
  }

  private var delayTask: Cancellable = _

  final def schedule(timeout: FiniteDuration, msg: Any): Cancellable = {
    context.system.scheduler.scheduleOnce(timeout, self, msg)(context.system.dispatcher)
  }

  final def scheduleDelay(delay: FiniteDuration): Unit = {
    if (configuration.automatic) delayTask = schedule(delay, CanContinueAfterDelayEvent)
  }

  final def cancelDelay(): Unit = {
    delayTask.cancel()
  }

  private val receiveCommonLeafMessages: Receive = receiveRecipient orElse receiveUnhandled

  final def becomeNow(receive: Receive): Unit = context.become(receive)

  def composeWithDefault(receive: Receive): Receive = receive orElse receiveCommonLeafMessages
}

/**
 * Base trait of node-like traffic system components (composing some members)
 */
trait BaseNodeActor extends BaseLeafActor {

  def receiveWhenIdle: Receive
  def memberProps: Iterable[Props]

  final def members: Map[Id, ActorRef] = _members
  final def memberIds: Seq[Id] = _memberIds

  private var _members: Map[Id, ActorRef] = Map()
  private var _memberIds: Seq[Id] = _

  override def preStart = {
    for (props <- memberProps) {
      val member = context.actorOf(props, getIdFromProps(props))
      member ! RegisterRecipientCommand(self)
    }
  }

  private def getIdFromProps(props: Props): Id = props.args.headOption match {
    case Some(id: Id) => id
    case _            => "member"
  }

  private var timeoutTask: Cancellable = _

  final def scheduleTimeout(timeout: FiniteDuration): Unit = {
    timeoutTask = schedule(timeout, TimeoutEvent)
  }

  final def cancelTimeout(): Unit = {
    timeoutTask.cancel()
  }

  /////////////////////////////////////////////////////////////////
  // STATE 0: INITIALIZING, WAITING FOR ALL MEMBERS REGISTRATION //
  /////////////////////////////////////////////////////////////////
  private val receiveWhenInitializing: Receive = {

    case RecipientRegisteredEvent(id) =>
      _members.get(id) match {
        case Some(_) =>
          log.error(s"Node ${this.id}: member DUPLICATED $id")
        case None =>
          _members = _members + (id -> sender())
          _memberIds = _members.keys.toSeq
          log.debug(s"Node ${this.id}: new member registered $id")
          if (_members.size == memberProps.size) {
            cancelTimeout()
            log.info(s"Node ${this.id} initialized. Members: ${memberIds.mkString(",")}")
            becomeNow(receiveWhenIdle)
          }
      }

    case TimeoutEvent =>
      log.warning(s"Timeout ocurred. Node ${this.id} PARTIALLY initialized. Members: ${memberIds.mkString(",")}")
      becomeNow(receiveWhenIdle)
  }

  private val receiveTick: Receive = {
    case TickEvent => members ! TickEvent
  }

  private val receiveCommonNodeMessages: Receive = receiveTick

  override val receive = composeWithDefault(receiveWhenInitializing)

  override def composeWithDefault(receive: Receive): Receive = super.composeWithDefault(receive orElse receiveCommonNodeMessages)

  scheduleTimeout(configuration.timeout)

}

/**
 * Base trait of node-like traffic system components (wrapping only one member)
 */
trait SingleNodeActor extends BaseNodeActor {

  def memberProp: Props

  final override val memberProps = Seq(memberProp)
  final def member: ActorRef = members.head._2

}

