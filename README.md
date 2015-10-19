[![Build Status](https://semaphoreci.com/api/v1/projects/c543ecbe-9aeb-4dde-932a-4cbfd3976d59/398717/badge.svg)](https://semaphoreci.com/arturopala/traffic-lights-control)      

#Traffic lights control system


## Goal

Traffic lights control system modelled using Akka Actors.

## Technologies

-   Scala
-   Akka Actors - 2.4.x
-   Akka HTTP - 1.0.x
-   WebSockets
-   React.js + Redux

## Build

Project uses SBT for backend stuff and Webpack for front-end.

```
npm install
sbt test:compile
```

## Test

```
sbt test
```

## Run

```sbt run```

Http server runs at <http://localhost:8080/>

## API

-   GET /api/layouts/{systemId} : returns system layout as JSON

-   GET /api/lights/{systemId} : returns system status as JSON
-   GET /api/lights/{systemId}/{lightId} : returns light state as JSON

-   GET /ws/lights : live stream of all light state events
-   GET /ws/lights/{systemId} : live stream of light state events for systemId = {systemId}
-   GET /ws/lights/{systemId}/{lightId} : live stream of light state events for light = systemId_lightId
