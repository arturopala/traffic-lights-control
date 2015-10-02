package trafficlightscontrol.model

sealed abstract class LightState(val colour: String, val id: String) {
  override val toString: String = s"${colour}"
}

case object RedLight extends LightState("Red", "R")
case object GreenLight extends LightState("Green", "G")
case object ChangingToRedLight extends LightState("YellowThenRed", "YTR")
case object ChangingToGreenLight extends LightState("YellowThenGreen", "YTG")