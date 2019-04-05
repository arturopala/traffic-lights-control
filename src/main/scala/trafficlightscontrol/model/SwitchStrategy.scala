package trafficlightscontrol.model

import scala.collection.Seq

trait SequenceStrategy {
  def name: String
  def apply(current: Id, members: Seq[Id]): Id
}

object SequenceStrategy {

  val RoundRobinSeqPattern = "RoundRobin\\((.*?)\\)".r

  def apply(byName: String): SequenceStrategy = byName match {
    case "RoundRobin"               => RoundRobin
    case RoundRobinSeqPattern(args) => roundRobin(args.split(','): _*)
  }

  val RoundRobin: SequenceStrategy = new SequenceStrategy {

    val name = "RoundRobin"
    def apply(current: Id, members: scala.collection.Seq[Id]): Id =
      members.indexOf(current) match {
        case -1 =>
          members.head
        case n => members((n + 1) % members.size)
      }
  }

  def roundRobin(ids: Id*): SequenceStrategy = new SequenceStrategy {
    val name = "RoundRobin" + ids.mkString("(", ",", ")")
    var index = -1
    val limit = ids.size
    def apply(current: Id, members: scala.collection.Seq[Id]): Id = {
      def findNext(counter: Int = 0): Id = {
        index = (index + 1) % limit
        val suffix = "_" + ids(index)
        members.find(i => i.endsWith(suffix)) match {
          case Some(id)                  => id
          case None if (counter < limit) => findNext(counter + 1)
          case None                      => current
        }
      }
      findNext()
    }
  }
}
