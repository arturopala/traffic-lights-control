package trafficlightscontrol

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class GetDisvcBaseJsScenario extends Simulation with Requests {

  val scn = scenario("GetStatus")
    .exec(getStatus.check(jsonPath("$.status").find.exists))

  setUp(
    scn
      .inject(
        atOnceUsers(1),
        nothingFor(5 seconds),
        rampUsers(1000) over (5 seconds)))
    .protocols(httpConf)
    .maxDuration(5.minutes)
}
