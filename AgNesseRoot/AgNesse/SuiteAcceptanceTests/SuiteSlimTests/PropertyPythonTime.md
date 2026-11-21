{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Python receives a datetime as text

The wiki draws the clock and the instant and sends each as a Slim string. `echo` returns that string. The clock check expects the text the `now` column drew. The instant check expects `other`, so the result records `2024-03-10T07:00:00Z`. A blank `now` on that second table is filled from the seed and is not written back.

````agnesse
define PropertyTimePy
```agnesse
define COMMAND_PATTERN
python3 AgNesseRoot/files/agnesse/tiny_slim.py
```

```agnesse
define SLIM_PORT
1
```

```agnesse
define zone
UTC
```

| script | tiny |

| scenario | show clock | clock |
| check | echo | @clock | 2024-03-10T06:00:00Z |

| property | show clock |
| trials | 1 |
| seed | 1 |
| now | 2024-03-10T06:00:00Z |
| clock | now |

| scenario | show instant | instant |
| check | echo | @instant | other |

| property | show instant |
| trials | 1 |
| seed | 1 |
| instant | datetime:2024-03-10T07:00:00Z..2024-03-10T07:00:00Z |
````

| script | page driver |
| --- | --- |
| given page | PropertyTimePy | with content | ${PropertyTimePy} |
| make | PropertyTimePy | a test page |
| check | request page | PropertyTimePy?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content matches | "right":1[,}] |
| ensure | content contains | "actual":"2024-03-10T07:00:00Z" |
| ensure | content contains | "actual":"2024-03-10T06:00:00Z" |
| ensure | content contains | "expected":"other" |
| ensure | content contains | 2000-01-01T00:00:01Z |
| ensure | source contains; | PropertyTimePy | datetime:2024-03-10T07:00:00Z..2024-03-10T07:00:00Z |
| reject | source contains; | PropertyTimePy | 2000-01-01T00:00:01Z |
| reject | source contains; | PropertyTimePy | drew |

| script | result view |
| --- | --- |
| show | rendered result |
