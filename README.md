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

+ main components are Actors:
  + traffic lights [TrafficLight]
  + lights groups [LightsGroupWithOnlyOneIsGreenStrategy]
  + detectors [TrafficDetector]
  + route directors [TrafficDirector]

##How to run demo app?

run command:

  $sbt re-start
  
open url in browser:

  http://localhost:8080/




