#Traffic lights control simulator

Project proposed and implemented during [Scala Hackaton in Berlin](http://www.meetup.com/Scala-Berlin-Brandenburg/events/213681812/)

##Requirements:

+ models behaviour of traffic lights 
+ works in real-time
+ can optimise throughput using different kinds of detectors
+ street layout is configurable

##Technologies:
+ Scala
+ Akka (main logic flow)
+ Spray (http service)
+ Html5 + AngularJs (frontend)
+ ScalaTest (tests)
+ Akka TestKit (tests)
+ SBT (build)

##Design:

+ all main components are Actors:
  + traffic lights [TrafficLight](https://github.com/arturopala/traffic-lights-control/blob/master/src/main/scala/trafficlightscontrol/TrafficLights.scala) models single traffic lights box with red, orange and green colours,
  + lights groups [LightsGroupWithOnlyOneIsGreenStrategy](https://github.com/arturopala/traffic-lights-control/blob/master/src/main/scala/trafficlightscontrol/LightsControllerWithOnlyOneIsGreenStrategy.scala) groups traffic lights and governs them with "Only one is green at the same time" strategy,
  + detectors [TrafficDetector](https://github.com/arturopala/traffic-lights-control/blob/master/src/main/scala/trafficlightscontrol/TrafficDetector.scala)
  + route directors [TrafficDirector](https://github.com/arturopala/traffic-lights-control/blob/master/src/main/scala/trafficlightscontrol/TrafficDirector.scala)
  + demo traffic system [DemoTrafficSystem](https://github.com/arturopala/traffic-lights-control/blob/master/src/main/scala/trafficlightscontrol/DemoTrafficSystem.scala)
  + http service [HttpServiceActor](https://github.com/arturopala/traffic-lights-control/blob/master/src/main/scala/trafficlightscontrol/Boot.scala)
  
+ 

##How to run demo app?

run command: 
    $sbt re-start

[open url in browser](http://localhost:8080/)




