package trafficlightscontrol

object SwitchStrategy {

  val RoundRobin: SwitchStrategy = (currentGreenId: String, memberIds: Seq[String]) => {
    memberIds.indexOf(currentGreenId) match {
      case -1 => memberIds.head
      case n  => memberIds((n + 1) % memberIds.size)
    }
  }
}