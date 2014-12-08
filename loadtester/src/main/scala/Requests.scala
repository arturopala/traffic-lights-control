package trafficlightscontrol

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import io.gatling.http.request.Body
import com.ning.http.client.RequestBuilder
import io.gatling.core.validation.Validation
import scala.reflect.ClassTag

trait Requests {

  val baseUrl = Option(System.getenv("LOADTEST_BASEURL")).getOrElse("http://localhost:8080")

  val httpConf = http
    .baseURL(baseUrl)
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0")

  val jsonHeaders = Map(
    "Content-Type" -> """application/json""",
    "Accept" -> """application/json""")

  val getStatus = http("get_status").get("/status").headers(jsonHeaders)

}
