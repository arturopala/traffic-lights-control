package trafficlightscontrol.model

sealed class HistoryEvent(val time: Long = System.nanoTime())

object HistoryEvent {
  case class Installed() extends HistoryEvent
  case class Started() extends HistoryEvent
  case class Terminated() extends HistoryEvent
}

case class SystemHistory(events: List[HistoryEvent] = Nil) {
  def installed(): SystemHistory = copy(events = HistoryEvent.Installed() :: events)
  def started(): SystemHistory = copy(events = HistoryEvent.Started() :: events)
  def terminated(): SystemHistory = copy(events = HistoryEvent.Terminated() :: events)
}