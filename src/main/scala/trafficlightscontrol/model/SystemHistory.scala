package trafficlightscontrol.model

sealed class HistoryEvent(val time: Long = System.nanoTime())

case class Installed() extends HistoryEvent
case class Started() extends HistoryEvent
case class Stopped() extends HistoryEvent
case class Terminated() extends HistoryEvent

case class SystemHistory(events: List[HistoryEvent] = Nil) {
  def installed(): SystemHistory = copy(events = Installed() :: events)
  def started(): SystemHistory = copy(events = Started() :: events)
  def stopped(): SystemHistory = copy(events = Stopped() :: events)
  def terminated(): SystemHistory = copy(events = Terminated() :: events)
}