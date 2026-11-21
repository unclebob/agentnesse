{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Python reads a dependent pair and a record

The wiki draws the cells and sends them as strings. `endIsAfterStart` answers `yes` when the end is greater than the start, so a claim of `no` fails and shrinks to 0 and 1. The same claim of `yes` passes. `kind` reads the JSON object. The claim expects `other`, and the result is `id`.

````agnesse
define PropertyPyDepend
```agnesse
define COMMAND_PATTERN
python3 AgNesseRoot/files/agnesse/tiny_slim.py
```

```agnesse
define SLIM_PORT
1
```

| script | tiny |

| scenario | order | start | stays after | end |
| check | end is after start; | @start | @end | no |

| property | order stays after |
| trials | 1 |
| seed | 1 |
| start | int:0..5 |
| end | int after start:0..6 |

| scenario | order | start | stays above | end |
| check | end is after start; | @start | @end | yes |

| property | order stays above |
| trials | 4 |
| seed | 1 |
| start | int:0..5 |
| end | int after start:0..9 |

| scenario | show kind | person |
| check | kind | @person | other |

| property | show kind |
| trials | 1 |
| seed | 1 |
| person | record:name one of:Ada; doc record:(kind one of:id) |
````

| script | page driver |
| --- | --- |
| given page | PropertyPyDepend | with content | ${PropertyPyDepend} |
| make | PropertyPyDepend | a test page |
| check | request page | PropertyPyDepend?test&format=json | 200 |
| ensure | content contains | "wrong":2 |
| ensure | content matches | "right":1[,}] |
| ensure | content contains | "actual":"0" |
| ensure | content contains | "actual":"1" |
| ensure | content contains | "actual":"yes" |
| ensure | content contains | "expected":"no" |
| ensure | content contains | "actual":"id" |
| ensure | content contains | "expected":"other" |
| ensure | content contains | shrunk |
| ensure | source contains; | PropertyPyDepend | int after start:0..6 |
| ensure | source contains; | PropertyPyDepend | record:(kind one of:id) |
| reject | source contains; | PropertyPyDepend | \| 0 \| 1 \| |

| script | result view |
| --- | --- |
| show | rendered result |
