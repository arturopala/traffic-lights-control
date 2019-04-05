package trafficlightscontrol.model

import org.scalatest.{FlatSpecLike, Matchers}
import trafficlightscontrol.model._

class SequenceStrategySpec extends FlatSpecLike with Matchers {

  "A SequenceStrategy.RoundRobin" should "return next member id" in {
    val strategy = SequenceStrategy.RoundRobin
    val members = Seq("b", "a", "c", "x")
    strategy("a", members) should be("c")
    strategy("c", members) should be("x")
    strategy("x", members) should be("b")
    strategy("b", members) should be("a")
    strategy("b", members) should be("a")
    strategy("a", members) should be("c")
    strategy("b", members) should be("a")
    strategy("x", members) should be("b")
  }

  "A SequenceStrategy.RoundRobin(...)" should "return next member id from the given ids sequence" in {
    val ids = Seq("a", "b", "e", "x", "f")
    val strategy = SequenceStrategy.roundRobin(ids: _*)
    val members = Seq("foo_b", "foo_a", "foo_c", "foo_x")
    strategy("foo_a", members) should be("foo_a")
    strategy("foo_a", members) should be("foo_b")
    strategy("foo_a", members) should be("foo_x")
    strategy("foo_a", members) should be("foo_a")
    strategy("bla", members) should be("foo_b")
    strategy("bla", members) should be("foo_x")
  }

  it should "terminate and return currentId if none of ids matches members" in {
    val ids = Seq("a", "b", "e", "x", "f")
    val strategy = SequenceStrategy.roundRobin(ids: _*)
    val members = Seq("foo_1", "foo_2", "foo_3", "foo_4")
    strategy("foo_1", members) should be("foo_1")
    strategy("foo_2", members) should be("foo_2")
    strategy("foo_3", members) should be("foo_3")
    strategy("foo_4", members) should be("foo_4")
    strategy("foo_1", members) should be("foo_1")
    strategy("bla", members) should be("bla")
  }

  it should "terminate and return currentId if only one member exists" in {
    val strategy = SequenceStrategy.RoundRobin
    val members = Seq("foo_1")
    strategy("foo_1", members) should be("foo_1")
    strategy("foo_1", members) should be("foo_1")
    strategy("foo_2", members) should be("foo_1")
    strategy("foo_3", members) should be("foo_1")
    strategy("foo_4", members) should be("foo_1")
    strategy("foo_1", members) should be("foo_1")
    strategy("bla", members) should be("foo_1")
  }

  "A SequenceStrategy" should "parse \"RoundRobin\" as a RoundRobin strategy" in {
    SequenceStrategy("RoundRobin") should be(SequenceStrategy.RoundRobin)
  }

  it should "parse \"RoundRobin(1,2,3)\" as a roundRobin(\"1\",\"2\",\"3\") strategy" in {
    SequenceStrategy("RoundRobin(1,2,3)").name shouldBe "RoundRobin(1,2,3)"
    SequenceStrategy("RoundRobin(a,b,c,e,d,x)").name shouldBe "RoundRobin(a,b,c,e,d,x)"
  }

}
