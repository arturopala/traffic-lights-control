package trafficlightscontrol.model

import scala.concurrent.duration._

case class Configuration(
  interval: FiniteDuration,
  delayRedToGreen: FiniteDuration,
  delayGreenToRed: FiniteDuration,
  sequenceDelay: FiniteDuration,
  timeout: FiniteDuration,
  automatic: Boolean = true,
  groupOffset: Option[FiniteDuration] = None) {
  def withLightDelay(d: FiniteDuration): Configuration = this.copy(delayRedToGreen = d, delayGreenToRed = d)
  def withGroupOffset(o: FiniteDuration): Configuration = this.copy(groupOffset = Option(o))
}

object Configuration {

  def apply(interval: FiniteDuration): Configuration = Configuration(
    interval = interval,
    delayRedToGreen = interval / 4,
    delayGreenToRed = interval / 6,
    sequenceDelay = interval / 10,
    timeout = interval * 10
  )

  def default = Configuration(10.seconds)
}
