package trafficlightscontrol

package object model {

  type Id = String
  type SwitchStrategy = (String, scala.collection.Seq[String]) => String

}
