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

Each traffic control component receives commands: ChangeToRedCommand, ChangeToGreenCommand and signal events: ChangedToRedEvent, ChangedToGreenEvent.

Each component also consumes, produces or passes TickEvents.

-   **Light**: the most primitive component of traffic system control taking one of four states sequentially: RedLight, ChangingToGreenLight, GreenLight, ChangingToRedLight

-   **Group**: set of components amongst which all are changing to the same state in the same time (all becomes Red or all becomes Green)

-   **Sequence**: set of components amongst which only one can become Green at the same time, the rest have to become Red.

-   **Offset**: component wrapper delaying commands

-   **Pulse**: component consuming TickEvents and generating ChangeToGreenCommands

-   **Switch**: component consuming TickEvents and generating alternately ChangeToGreenCommand and ChangeToRedCommand

All statefull components reports StateChangeEvent to the event listener.

#### Demo traffic system definition:

```scala
Sequence("s1", SequenceStrategy.RoundRobin,
      Sequence("s2", SequenceStrategy.RoundRobin,
        Group(
          "g1",
          Light("l1", RedLight),
          Offset(
            500.millis,
            Light("l2", GreenLight)
          )
        ),
        Group(
          "g2",
          Light("l3", GreenLight),
          Offset(
            1.second,
            Light("l4", RedLight)
          )
        ),
        Group(
          "g3",
          Light("l5", GreenLight),
          Offset(
            1.second,
            Light("l6", RedLight)
          ),
          Sequence("s3", SequenceStrategy.RoundRobin,
            Light("l7", RedLight),
            Light("l8", RedLight),
            Light("l9", RedLight))
        )),
      Sequence("s12", SequenceStrategy.RoundRobin,
        Group(
          "g11",
          Offset(
            500.millis,
            Light("l12", GreenLight)
          ),
          Light("l11", RedLight)
        ),
        Group(
          "g12",
          Offset(
            1.second,
            Sequence("s14", SequenceStrategy.RoundRobin,
              Light("l14a", RedLight),
              Light("l14b", RedLight))
          ),
          Light("l13", GreenLight)
        ),
        Group(
          "g13",
          Light("l15", GreenLight),
          Light("l16", RedLight),
          Offset(
            1.second,
            Sequence("s13", SequenceStrategy.RoundRobin,
              Light("l17", RedLight),
              Light("l19", RedLight))
          )
        )))
```


