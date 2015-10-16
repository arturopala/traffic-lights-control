package trafficlightscontrol.http

import spray.json._
import DefaultJsonProtocol._

import trafficlightscontrol.model._
import trafficlightscontrol.actors._

import scala.concurrent.duration._

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

  val FiniteDurationFormatPattern = """(\d+?)(?:\s|\.)?([a-z]*?)""".r

  implicit object FiniteDurationFormat extends RootJsonFormat[FiniteDuration] {
    def write(fd: FiniteDuration) = JsString(fd.toString)
    def read(value: JsValue) = value match {
      case JsString(s) => s match {
        case FiniteDurationFormatPattern(l, u) => FiniteDuration(l.toLong, if (u.isEmpty) "ms" else u)
        case s                                 => FiniteDuration(s.toLong, MILLISECONDS)
      }
      case JsNumber(l) => FiniteDuration(l.toLong, MILLISECONDS)
      case _           => deserializationError("FiniteDuration string or number expected")
    }
  }

  implicit object ComponentJsonFormat extends RootJsonFormat[Component] {
    def write(component: Component): JsValue = component match {
      case Light(id, initialState)                   => JsObject("type" -> JsString("light"), "id" -> JsString(id), "state" -> initialState.toJson)
      case Group(id, members @ _*)                   => JsObject("type" -> JsString("group"), "id" -> JsString(id), "members" -> members.toJson)
      case Sequence(id, strategy, members @ _*)      => JsObject("type" -> JsString("sequence"), "id" -> JsString(id), "strategy" -> strategy.toJson, "members" -> members.toJson)
      case Switch(member, initiallyGreen, skipTicks) => JsObject("type" -> JsString("switch"), "initially" -> JsBoolean(initiallyGreen), "skip" -> JsNumber(skipTicks), "member" -> member.toJson)
      case Pulse(member, skipTicks)                  => JsObject("type" -> JsString("pulse"), "skip" -> JsNumber(skipTicks), "member" -> member.toJson)
      case Offset(offset, member)                    => JsObject("type" -> JsString("offset"), "offset" -> offset.toJson, "member" -> member.toJson)
    }
    def read(value: JsValue): Component = value.asJsObject.getFields("type") match {
      case Seq(JsString(elementType)) => elementType match {
        case "light" => value.asJsObject.getFields("id", "state") match {
          case Seq(JsString(id), state) => Light(id, state.convertTo[LightState])(Configuration.default)
        }
        case "group" => value.asJsObject.getFields("id", "members") match {
          case Seq(JsString(id), members) => Group(id, (members.convertTo[Seq[Component]]): _*)(Configuration.default)
        }
        case "sequence" => value.asJsObject.getFields("id", "strategy", "members") match {
          case Seq(JsString(id), strategy, members) => Sequence(id, strategy.convertTo[SequenceStrategy], (members.convertTo[Seq[Component]]): _*)(Configuration.default)
        }
        case "switch" => value.asJsObject.getFields("member", "initially", "skip") match {
          case Seq(member, initially, skip) => Switch(member.convertTo[Component], initially.convertTo[Boolean], skip.convertTo[Int])(Configuration.default)
        }
        case "pulse" => value.asJsObject.getFields("member", "skip") match {
          case Seq(member, skip) => Pulse(member.convertTo[Component], skip.convertTo[Int])(Configuration.default)
        }
        case "offset" => value.asJsObject.getFields("member", "offset") match {
          case Seq(member, offset) => Offset(offset.convertTo[FiniteDuration], member.convertTo[Component])(Configuration.default)
        }
      }

      case _ => deserializationError("Component, one of {Light,Sequence,Group} expected")
    }
  }

}