{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# An end stays after its start

`int after start` is at least one above `start`. The claim expects the word `same`, so the drawn pair fails. Seed 1 shrinks to 0 and 1, and 1 is still after 0. A reference to a later column, and a column that names itself, are refused before any trial runs.

````agnesse
define PropertyEnds
```agnesse
define SLIM_PORT
1
```

| script | agnesse.slim.test.TestSlim |

| scenario | order | start | stays after | end |
| check | echo string | @end | same |

| property | order stays after |
| trials | 1 |
| seed | 1 |
| start | int:0..5 |
| end | int after start:0..6 |
````

| script | page driver |
| --- | --- |
| given page | PropertyEnds | with content | ${PropertyEnds} |
| make | PropertyEnds | a test page |
| check | request page | PropertyEnds?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content contains | "right":0 |
| ensure | content contains | "actual":"0" |
| ensure | content contains | "actual":"1" |
| ensure | content contains | "expected":"same" |
| ensure | content contains | shrunk |
| ensure | source contains; | PropertyEnds | int after start:0..6 |
| ensure | source contains; | PropertyEnds | seed \| 1 |
| reject | source contains; | PropertyEnds | \| 0 \| 1 \| |

| script | result view |
| --- | --- |
| show | rendered result |

````agnesse
define PropertyLater
```agnesse
define SLIM_PORT
1
```

| property:agnesse.slim.test.TestSlim |
| end | start | return int? |
| int after start | int:0..3 | 7 |

| property:agnesse.slim.test.TestSlim |
| end | return int? |
| int after end | 7 |
````

| script | page driver |
| --- | --- |
| given page | PropertyLater | with content | ${PropertyLater} |
| make | PropertyLater | a test page |
| check | request page | PropertyLater?test&format=json | 200 |
| ensure | content contains | "wrong":0 |
| ensure | content contains | 2 exceptions |
| ensure | content contains | refers to later column |
| ensure | content contains | cycle in column |
| ensure | source contains; | PropertyLater | int after start |
| ensure | source contains; | PropertyLater | int after end |

| script | result view |
| --- | --- |
| show | rendered result |
