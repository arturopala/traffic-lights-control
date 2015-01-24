package trafficlightscontrol

import java.nio.charset.Charset
import java.nio.file.{ FileSystems, Files, Path }
import java.util.function.Consumer
import org.junit.runner.RunWith
import org.scalatest.{ Finders, FlatSpecLike, Matchers }
import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestActorRef, TestKit }
import org.scalatest.junit.JUnitRunner
import com.typesafe.config.ConfigFactory
import akka.testkit.TestProbe
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import java.util.concurrent.atomic.AtomicInteger
import akka.actor.ActorRef

@RunWith(classOf[JUnitRunner])
class WebSocketSpec extends FlatSpecLike with Matchers {

  "A ws.RouteMatcher" should "match using single wildcard" in {
    val m = ws.RouteMatcher("*", 5)
    m.isDefinedAt("/foo/o5/a") should be(true)
    m.isDefinedAt("/foo/o5/a/") should be(true)
    m.isDefinedAt("/") should be(true)
    m.isDefinedAt("foo") should be(true)
    m.isDefinedAt("") should be(true)
  }

  it should "match using prefix+wildcard" in {
    val m = ws.RouteMatcher("/f*", 5)
    m.isDefinedAt("/foo/o5/a") should be(true)
    m.isDefinedAt("/f") should be(true)
    m.isDefinedAt("/f/f/f/f/f") should be(true)
    m.isDefinedAt("/boo/o5/a") should be(false)
    m.isDefinedAt("foo/o5/a") should be(false)
    m.isDefinedAt("/b/foo/o5/a") should be(false)
    m.isDefinedAt("/") should be(false)
    m.isDefinedAt("") should be(false)
    m.isDefinedAt("*") should be(false)
  }

  it should "match using wildcard+suffix" in {
    val m = ws.RouteMatcher("*a", 5)
    m.isDefinedAt("/foo/o5/a") should be(true)
    m.isDefinedAt("a") should be(true)
    m.isDefinedAt("/a") should be(true)
    m.isDefinedAt("/foo/o5/") should be(false)
    m.isDefinedAt("/foo/o5/a/") should be(false)
    m.isDefinedAt("") should be(false)
    m.isDefinedAt("*") should be(false)
  }

  it should "match using prefix+wildcard+suffix" in {
    val m = ws.RouteMatcher("/f*a", 5)
    m.isDefinedAt("/foo/o5/a") should be(true)
    m.isDefinedAt("/fa") should be(true)
    m.isDefinedAt("/fyr/ueyf/ueufb/77868/uyfua") should be(true)
    m.isDefinedAt("/f") should be(false)
    m.isDefinedAt("/f/f/f/f/f") should be(false)
    m.isDefinedAt("/boo/o5/a") should be(false)
    m.isDefinedAt("foo/o5/a") should be(false)
    m.isDefinedAt("/b/foo/o5/a") should be(false)
    m.isDefinedAt("a") should be(false)
    m.isDefinedAt("/a") should be(false)
    m.isDefinedAt("/foo/o5/") should be(false)
    m.isDefinedAt("/foo/o5/a/") should be(false)
    m.isDefinedAt("/") should be(false)
    m.isDefinedAt("") should be(false)
    m.isDefinedAt("*") should be(false)
  }

  "A ws.Routes" should "prepare Route from mappings" in {
    val mappings = Seq("/a" -> 1, "/aa" -> 2, "/a/b" -> 3)
    val route = ws.Routes(mappings: _*)
    route("/a/b") should be(3)
    route("/aa") should be(2)
    route("/a") should be(1)
    an[Exception] should be thrownBy route("/f")
  }

}
