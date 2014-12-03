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
import spray.testkit.ScalatestRouteTest
import spray.routing.HttpService
import spray.http.StatusCodes._
import spray.http.ContentTypes

@RunWith(classOf[JUnitRunner])
class HttpServiceSpec extends FlatSpecLike with Matchers
    with ScalatestRouteTest with ActorSystemTestKit {

  val module = new Module

  "HttpService" should "return a json report for GET /status" in {
    Get("/status") ~> module.httpService.route ~> check {
      responseAs[String] should startWith("{\"status\":")
      status === OK
      contentType === ContentTypes.`application/json`
    }
  }

}
