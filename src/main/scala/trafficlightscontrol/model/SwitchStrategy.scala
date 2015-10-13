package trafficlightscontrol.model

import scala.collection.Seq

trait SequenceStrategy {
  def name: String
  def apply(current: Id, members: Seq[Id]): Id
}

object SequenceStrategy {

  def apply(byName: String): SequenceStrategy = byName match {
    case "RoundRobin" => RoundRobin
  }

  val RoundRobin: SequenceStrategy = new SequenceStrategy {

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