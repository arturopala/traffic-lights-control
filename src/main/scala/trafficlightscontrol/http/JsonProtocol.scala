package trafficlightscontrol.http

import spray.json._
import DefaultJsonProtocol._

import trafficlightscontrol.model._
import trafficlightscontrol.actors._

object JsonProtocol extends DefaultJsonProtocol {

  implicit object LightStateJsonFormat extends RootJsonFormat[LightState] {
    def write(lightState: LightState): JsValue = JsString(lightState.id)
    def read(value: JsValue): LightState = value match {
      case JsString(id) => id match {
        case RedLight.id             => RedLight
        case GreenLight.id           => GreenLight
        case ChangingToRedLight.id   => ChangingToRedLight
        case ChangingToGreenLight.id => ChangingToGreenLight
      }
      case _ => deserializationError("LightState expected")
    }
  }

  implicit val ReportEventJsonFormat = jsonFormat1(ReportEvent.apply)
  implicit val StatusEventJsonFormat = jsonFormat2(StatusEvent.apply)

  implicit object SequenceStrategyJsonFormat extends RootJsonFormat[SequenceStrategy] {
    def write(strategy: SequenceStrategy): JsValue = JsString(strategy.name)
    def read(value: JsValue): SequenceStrategy = value match {
      case JsString(name) => SequenceStrategy(name)
      case _              => deserializationError("SequenceStrategy name expected")
    }
  }

  implicit object ComponentJsonFormat extends RootJsonFormat[Component] {
    def write(component: Component): JsValue = component match {
      case Light(id, initialState)                       => JsObject("type" -> JsString("Light"), "id" -> JsString(id), "state" -> initialState.toJson)
      case Group(id, members @ _*)                       => JsObject("type" -> JsString("Group"), "id" -> JsString(id), "members" -> members.toJson)
      case Sequence(id, strategy, members @ _*)          => JsObject("type" -> JsString("Sequence"), "id" -> JsString(id), "strategy" -> strategy.toJson, "members" -> members.toJson)
      case Switch(id, member, initiallyGreen, skipTicks) => JsObject("type" -> JsString("Switch"), "id" -> JsString(id), "initially" -> JsBoolean(initiallyGreen), "skip" -> JsNumber(skipTicks), "member" -> member.toJson)
      case Pulse(id, member, skipTicks)                  => JsObject("type" -> JsString("Pulse"), "id" -> JsString(id), "skip" -> JsNumber(skipTicks), "member" -> member.toJson)
    }
    def read(value: JsValue): Component = value.asJsObject.getFields("type", "id") match {
      case Seq(JsString(elementType), JsString(id)) => elementType match {
        case "Light" => value.asJsObject.getFields("state") match {
          case Seq(state) => Light(id, state.convertTo[LightState])(Configuration.default)
        }
        case "Group" => value.asJsObject.getFields("members") match {
          case Seq(members) => Group(id, (members.convertTo[Seq[Component]]): _*)(Configuration.default)
        }
        case "Sequence" => value.asJsObject.getFields("strategy", "members") match {
          case Seq(strategy, members) => Sequence(id, strategy.convertTo[SequenceStrategy], (members.convertTo[Seq[Component]]): _*)(Configuration.default)
        }
        case "Switch" => value.asJsObject.getFields("member", "initially", "skip") match {
          case Seq(member, initially, skip) => Switch(id, member.convertTo[Component], initially.convertTo[Boolean], skip.convertTo[Int])(Configuration.default)
        }
        case "Pulse" => value.asJsObject.getFields("member", "skip") match {
          case Seq(member, skip) => Pulse(id, member.convertTo[Component], skip.convertTo[Int])(Configuration.default)
        }
      }

      case _ => deserializationError("Component, one of {Light,Sequence,Group} expected")
    }
  }

}