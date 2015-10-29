[![Build Status](https://semaphoreci.com/api/v1/projects/c543ecbe-9aeb-4dde-932a-4cbfd3976d59/398717/badge.svg)](https://semaphoreci.com/arturopala/traffic-lights-control)      

#Traffic Lights Control system


## Goal

Traffic lights control system modelled using Akka Actors.

## Technologies

-   Scala
-   Akka Actors - 2.4.x
-   Akka HTTP - 1.0.x
-   WebSockets
-   React.js + Redux

### Build

Project uses SBT to build backend components and Webpack to pack front-end assets.

```
npm install
npm run build
sbt test:compile
```

### Test

```
sbt test
```

### Run

```sbt run```

Http server runs at <http://localhost:8080/>

## API

-   GET /api/layouts/{systemId} : returns system layout as JSON

-   GET /api/lights/{systemId} : returns system status as JSON
-   GET /api/lights/{systemId}/{lightId} : returns light state as JSON

-   GET /ws/lights : live stream of all light state events
-   GET /ws/lights/{systemId} : live stream of light state events for systemId
-   GET /ws/lights/{systemId}/{lightId} : live stream of light state events for systemId_lightId

## Model

Each traffic control component receives commands: ChangeToRedLight, ChangeToGreenLight and signal events: ChangedToRedLight, ChangedToGreenLight.

Each component also consumes, produces or passes TickEvents.

-   Light: the most primitive component of traffic system control taking one of four states sequentially: RedLight, ChangingToGreenLight, GreenLight, ChangingToRedLight

-   Group: set of components amongst which all are changing to the same state in the same time (all becomes Red or all becomes Green)

-   Sequence: set of components amongst which only one can become Green at the same time, the rest have to become Red.

-   Offset: component wrapper delaying commands

-   Pulse: component consuming TickEvents and generating ChangeToGreenCommands

-   Switch: component consuming TickEvents and generating alternately ChangeToGreenCommand and ChangeToRedCommand

All statefull components reports StateChangeEvent to the event listener.


