{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# A partition keeps its total

`partition:10 into 3` draws three non-negative ints that add to 10. The claim expects the word `same`, so the draw fails. Shrinking moves the later parts onto the first. Seed 1 draws `[2, 7, 1]` and ends at `[10, 0, 0]`.

````agnesse
define PropertyParts
```agnesse
define SLIM_PORT
1
```

| script | agnesse.slim.test.TestSlim |

| scenario | echo back | value |
| check | echo string | @value | same |

| property | echo back |
| trials | 1 |
| seed | 1 |
| value | partition:10 into 3 |
````

| script | page driver |
| --- | --- |
| given page | PropertyParts | with content | ${PropertyParts} |
| make | PropertyParts | a test page |
| check | request page | PropertyParts?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content contains | "right":0 |
| ensure | content contains | "actual":"[10, 0, 0]" |
| ensure | content contains | "expected":"same" |
| ensure | content contains | drew [2, 7, 1] |
| ensure | content contains | shrunk |
| ensure | source contains; | PropertyParts | partition:10 into 3 |
| reject | source contains; | PropertyParts | [10, 0, 0] |

| script | result view |
| --- | --- |
| show | rendered result |
