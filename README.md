[![Build Status](https://semaphoreci.com/api/v1/projects/c543ecbe-9aeb-4dde-932a-4cbfd3976d59/398717/badge.svg)](https://semaphoreci.com/arturopala/traffic-lights-control)      

#Traffic lights control simulator

Project proposed and implemented during [Scala Hackaton in Berlin](http://www.meetup.com/Scala-Berlin-Brandenburg/events/213681812/)

##[Backlog](https://trello.com/b/xTByYiHV/traffic-lights-manager)

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

##How to run demo app?

run command: 
    $sbt re-start

[open url in browser](http://localhost:8080/)

##Description

Traffic control app uses message passing between asynchronous actors as a tool to model real-time traffic lights system. Few commands, querries and events forms control and supervision protocol. Demo implemented at hackaton represents simple crossroad but we took approach that allows design systems of a larger scale. 



