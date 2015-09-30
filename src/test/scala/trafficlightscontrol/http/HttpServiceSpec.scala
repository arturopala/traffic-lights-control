package trafficlightscontrol.http

import java.nio.charset.Charset
import java.nio.file.{ FileSystems, Files, Path }
import java.util.function.Consumer

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
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers._

import trafficlightscontrol.model._
import trafficlightscontrol.actors._
import trafficlightscontrol.Module

import spray.json._
import DefaultJsonProtocol._

class HttpServiceSpec extends FlatSpecLike with Matchers with ScalatestRouteTest with ActorSystemTestKit {

  val module = new Module

  import JsonProtocol._

  "HttpService" should "return a json report for GET /status" in {
    Get("/status") ~> module.httpService.route ~> check {
      status === OK
      contentType === ContentTypes.`application/json`
      val body = responseAs[String].parseJson.convertTo[ReportEvent]
      body.report should not be null
    }
  }

  it should "return index.html page for GET /" in {
    Get("/") ~> module.httpService.route ~> check {
      status === OK
      contentType === ContentType(MediaTypes.`text/html`)
      responseAs[String] should include("""<title>Traffic Lights Control</title>""")
    }
  }

  it should "return traffic.css file for GET /traffic.css" in {
    Get("/traffic.css") ~> module.httpService.route ~> check {
      status === OK
      contentType === ContentType(MediaTypes.`text/css`)
      responseAs[String] should include("""margin: 0 auto;""")
    }
  }

}
