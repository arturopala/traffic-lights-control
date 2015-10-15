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

  private val receiveUnhandled: Receive = {
    case event: Event => ()
    case command: Command =>
      recipient ! MessageIgnoredEvent(command)
    case other =>
      log.debug(s"Component ${this.id}: message not recognized $other")
  }

  private var delayTask: Cancellable = _

  def scheduleDelay(delay: FiniteDuration): Unit = {
    if (configuration.automatic) delayTask = context.system.scheduler.scheduleOnce(delay, self, CanContinueAfterDelayEvent)(context.system.dispatcher)
  }

  def cancelDelay(): Unit = {
    delayTask.cancel()
  }

  val receiveCommonLeafMessages: Receive = receiveRecipient orElse receiveUnhandled

  def becomeNow(receive: Receive) = context.become(receive orElse receiveCommonLeafMessages)

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

  def scheduleTimeout(timeout: FiniteDuration): Unit = {
    timeoutTask = context.system.scheduler.scheduleOnce(timeout, self, TimeoutEvent)(context.system.dispatcher)
  }

  def cancelTimeout(): Unit = {
    timeoutTask.cancel()
  }

  /////////////////////////////////////////////////////////////////
  // STATE 0: INITIALIZING, WAITING FOR ALL MEMBERS REGISTRATION //
  /////////////////////////////////////////////////////////////////
  private val receiveWhenInitializing: Receive = {

    case RecipientRegisteredEvent(id) =>
      _members = _members + (id -> sender())
      _memberIds = _members.keys.toSeq
      log.debug(s"Node ${this.id}: new member registered $id")
      if (_members.size == memberProps.size) {
        cancelTimeout()
        log.info(s"Node ${this.id} initialized. Members: ${memberIds.mkString(",")}")
        context.become(receiveWhenIdle orElse receiveCommonNodeMessages)
      }

    case TimeoutEvent =>
      log.warning(s"Timeout ocurred. Node ${this.id} PARTIALLY initialized. Members: ${memberIds.mkString(",")}")
      context.become(receiveWhenIdle orElse receiveCommonNodeMessages)
  }

  private val receiveTick: Receive = {
    case TickEvent => members ! TickEvent
  }

  private val receiveCommonNodeMessages: Receive = receiveTick orElse receiveCommonLeafMessages

  override val receive = receiveWhenInitializing orElse receiveCommonNodeMessages

  override def becomeNow(receive: Receive) = super.becomeNow(receive orElse receiveCommonNodeMessages)

  scheduleTimeout(configuration.timeout)

}

/**
 * Base trait of node-like traffic system components (wrapping only one member)
 */
trait SingleNodeActor extends BaseNodeActor {

  def memberProp: Props
  final override val memberProps = Seq(memberProp)

}

