[![Build Status](https://semaphoreci.com/api/v1/projects/c543ecbe-9aeb-4dde-932a-4cbfd3976d59/398717/badge.svg)](https://semaphoreci.com/arturopala/traffic-lights-control)      

#Traffic lights control system


## Goal

Traffic lights control system modelled using Akka Actors.

## Technologies

-   Scala
-   Akka Actors - 2.4.x
-   Akka HTTP - 1.0.x
-   WebSockets

## Run

```sbt run```

Http server runs at <http://localhost:8080/>

## API

-   GET /api/lights : returns current system status as JSON
-   GET /api/lights/{id} : returns state of light for id = {id}

-   GET /ws/lights : live stream of all light state events
-   GET /ws/lights/{id} : live stream of light state events for id = {id}
