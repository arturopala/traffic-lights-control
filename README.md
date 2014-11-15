#Traffic lights control simulator

Project proposed and implemented during [Scala Hackaton in Berlin](http://www.meetup.com/Scala-Berlin-Brandenburg/events/213681812/)

##Requirements:

+ models behaviour of traffic lights 
+ works in real-time
+ can optimise throughput using different kinds of detectors
+ street layout is configurable

##Technologies:
+ Scala
+ Akka
+ Spray
+ Html5 + AngularJs (frontend)

##Design:

+ all main components are Actors:
  + traffic lights [TrafficLight](https://github.com/arturopala/traffic-lights-control/blob/master/src/main/scala/trafficlightscontrol/TrafficLights.scala)
  + lights groups [LightsGroupWithOnlyOneIsGreenStrategy](https://github.com/arturopala/traffic-lights-control/blob/master/src/main/scala/trafficlightscontrol/LightsControllerWithOnlyOneIsGreenStrategy.scala)
  + detectors [TrafficDetector](https://github.com/arturopala/traffic-lights-control/blob/master/src/main/scala/trafficlightscontrol/TrafficDetector.scala)
  + route directors [TrafficDirector](https://github.com/arturopala/traffic-lights-control/blob/master/src/main/scala/trafficlightscontrol/TrafficDirector.scala)
  + demo traffic system [DemoTrafficSystem](https://github.com/arturopala/traffic-lights-control/blob/master/src/main/scala/trafficlightscontrol/DemoTrafficSystem.scala)
  + http service [HttpServiceActor](https://github.com/arturopala/traffic-lights-control/blob/master/src/main/scala/trafficlightscontrol/Boot.scala)
  
+ 

##How to run demo app?

run command: $sbt re-start

open url in browser: (http://localhost:8080/)




