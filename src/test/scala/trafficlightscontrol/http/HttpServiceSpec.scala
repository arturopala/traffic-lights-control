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

  it should "return index.html page for GET /" in {
    Get("/") ~> module.httpService.route ~> check {
      status should be(OK)
      contentType should be(ContentType(MediaTypes.`text/html`, HttpCharsets.`UTF-8`))
      responseAs[String] should include("""<title>Traffic Lights Control</title>""")
    }
  }

  it should "return index.html page for GET /foo" in {
    Get("/foo") ~> module.httpService.route ~> check {
      status should be(OK)
      contentType should be(ContentType(MediaTypes.`text/html`, HttpCharsets.`UTF-8`))
      responseAs[String] should include("""<title>Traffic Lights Control</title>""")
    }
  }

  it should "return app.js file for GET /app.js" in {
    Get("/app.js") ~> module.httpService.route ~> check {
      status should be(OK)
      contentType should be(ContentType(MediaTypes.`application/javascript`, HttpCharsets.`UTF-8`))
      responseAs[String] should include("""function(module, exports) {""")
    }
  }

  it should "return style.css file for GET /style.css" in {
    Get("/style.css") ~> module.httpService.route ~> check {
      status should be(OK)
      contentType should be(ContentType(MediaTypes.`text/css`, HttpCharsets.`UTF-8`))
      responseAs[String] should include(""".light {""")
    }
  }

  "HttpService" should "return a json report for GET /lights" in {
    Get("/api/lights") ~> module.httpService.route ~> check {
      status should be(OK)
      contentType should be(ContentTypes.`application/json`)
      val body = responseAs[String].parseJson.convertTo[ReportEvent]
      body.report should not be null
    }
  }

  it should "return 404 for GET /lights/foo" in {
    Get("/api/lights/foo") ~> module.httpService.route ~> check {
      status should be(NotFound)
      responseAs[String] should be("""Light #foo not found!""")
    }
  }

  it should "return status for GET /lights/l1" in {
    val lightStatus = StatusEvent("l1", GreenLight)
    module.monitoring.actor ! lightStatus
    Get("/api/lights/l1") ~> module.httpService.route ~> check {
      status should be(OK)
      responseAs[String].parseJson.convertTo[StatusEvent] should be(lightStatus)
    }
  }

  it should "return layout json for GET /api/layouts/demo" in {
    Get("/api/layouts/demo") ~> module.httpService.route ~> check {
      status should be(OK)
      contentType should be(ContentTypes.`application/json`)
      val body = responseAs[String]
      val layout = body.parseJson.convertTo[Component]
      layout shouldBe module.initialLayouts("demo")
    }
  }

  /*import akka.http.scaladsl.model.ws._
  import akka.http.scaladsl.testkit.WSProbe

  it should "handle /ws/lights requests" in {
    val wsClient = WSProbe()
    WS("/ws/lights", wsClient.flow) ~> module.httpService.route ~>
      check {
        isWebsocketUpgrade shouldEqual true
        wsClient.sendMessage("Peter")
          wsClient.expectMessage("Hello Peter!")

          wsClient.sendMessage(BinaryMessage(ByteString("abcdef")))
          // wsClient.expectNoMessage() // will be checked implicitly by next expectation

          wsClient.sendMessage("John")
          wsClient.expectMessage("Hello John!")

        wsClient.sendCompletion()
        wsClient.expectCompletion()
      }
  }*/

}
