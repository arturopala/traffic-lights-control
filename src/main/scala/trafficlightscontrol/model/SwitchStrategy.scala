package trafficlightscontrol.model

object SwitchStrategy {

  val RoundRobin: SwitchStrategy = (currentGreenId: Id, memberIds: Seq[Id]) => {
    memberIds.indexOf(currentGreenId) match {
      case -1 =>
        memberIds.head
      case n => memberIds((n + 1) % memberIds.size)
    }
  }
}