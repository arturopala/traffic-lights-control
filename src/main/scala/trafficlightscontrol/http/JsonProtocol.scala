package trafficlightscontrol.http

import spray.json._
import DefaultJsonProtocol._

import trafficlightscontrol.model._
import trafficlightscontrol.actors._

object JsonProtocol extends DefaultJsonProtocol {

  implicit object LightStateJsonFormat extends RootJsonFormat[LightState] {
    def write(lightState: LightState) = JsString(lightState.id)
    def read(value: JsValue) = value match {
      case JsString(id) => id match {
        case RedLight.id             => RedLight
        case GreenLight.id           => GreenLight
        case ChangingToRedLight.id   => ChangingToRedLight
        case ChangingToGreenLight.id => ChangingToGreenLight
      }
      case _ => deserializationError("Color expected")
    }
  }

  implicit val ReportEventJsonFormat = jsonFormat1(ReportEvent.apply)
}