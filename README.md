#Traffic lights control simulator

Project proposed and implemented during [Scala Hackaton in Berlin](http://www.meetup.com/Scala-Berlin-Brandenburg/events/213681812/)

##[Backlog]()

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
  + lights groups [LightsGroupWithOnlyOneIsGreenStrategy](https://github.com/arturopala/traffic-lights-control/blob/master/src/main/scala/trafficlightscontrol/LightsControllerWithOnlyOneIsGreenStrategy.scala) groups traffic lights and governs them with "Only one may be green at the same time" strategy,
  + detectors [TrafficDetector](https://github.com/arturopala/traffic-lights-control/blob/master/src/main/scala/trafficlightscontrol/TrafficDetector.scala) provides size of the queue on the lane
  + route directors [TrafficDirector](https://github.com/arturopala/traffic-lights-control/blob/master/src/main/scala/trafficlightscontrol/TrafficDirector.scala) analyzes traffic and initializes lights change,
  + demo traffic system [DemoTrafficSystem](https://github.com/arturopala/traffic-lights-control/blob/master/src/main/scala/trafficlightscontrol/TrafficSystem.scala) models simple crossroad with 4 directions,
  + http service [HttpServiceActor](https://github.com/arturopala/traffic-lights-control/blob/master/src/main/scala/trafficlightscontrol/Boot.scala) provides /status resource with current traffic system state and index page with crossroad visualization.
  
+ all kinds of logic are implemented via messages flow (commands,querries,events) 

##How to run demo app?

run command: 
    $sbt re-start

[open url in browser](http://localhost:8080/)

##Description

Traffic control app uses message passing between asynchronous actors as a tool to model real-time traffic lights system. Few commands, querries and events forms control and supervision protocol. Demo implemented at hackaton represents simple crossroad but we took approach that allows design systems of a larger scale. 

The innermost primitive component of a traffic system is a [TrafficLight](https://github.com/arturopala/traffic-lights-control/blob/master/src/main/scala/trafficlightscontrol/TrafficLights.scala) actor simulating red/orange/green signals using finite state machine. TrafficLight processes ChangeToRed, ChangeToGreen and GetStatusQuery. Light switching with orange blink phase is handled internally with scheduler. Externally are exposed ACK events: ChangedToRedEvent and ChangedToGreenEvent. StatusEvent is used by [MonitoringActor](https://github.com/arturopala/traffic-lights-control/blob/master/src/main/scala/trafficlightscontrol/MonitoringActor.scala) to report system state via http service. TrafficLights are intended to be grouped by outer components like groups, managers, directors, etc.



