package trafficlightscontrol.model

import scala.collection.Seq

trait SwitchStrategy {
  def name: String
  def apply(current: Id, members: Seq[Id]): Id
}

object SwitchStrategy {

  def apply(byName: String): SwitchStrategy = byName match {
    case "RoundRobin" => RoundRobin
  }

  val RoundRobin: SwitchStrategy = new SwitchStrategy {

    val name = "RoundRobin"
    def apply(current: Id, members: scala.collection.Seq[Id]): Id = {
      members.indexOf(current) match {
        case -1 =>
          members.head
        case n => members((n + 1) % members.size)
      }
    }
  }
}