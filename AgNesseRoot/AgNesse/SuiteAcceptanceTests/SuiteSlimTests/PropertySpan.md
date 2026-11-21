{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# A span shrinks toward a zero-length start

`span:0..5 length 1..4` draws `start+length`, a half-open interval. The claim expects the word `same`, so the draw fails. Seed 1 draws `0+3` and shrinks to `0+1`. A list of spans may overlap, and the same seed draws `[3+1, 0+3]` and shrinks to `[0+1, 0+1]`.

````agnesse
define PropertySpanRow
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
| value | span:0..5 length 1..4 |

| property | echo back |
| trials | 1 |
| seed | 1 |
| value | list of span:0..5 length 1..3 size 2..2 |
````

| script | page driver |
| --- | --- |
| given page | PropertySpanRow | with content | ${PropertySpanRow} |
| make | PropertySpanRow | a test page |
| check | request page | PropertySpanRow?test&format=json | 200 |
| ensure | content contains | "wrong":2 |
| ensure | content contains | "right":0 |
| ensure | content contains | "actual":"0+1" |
| ensure | content contains | "actual":"[0+1, 0+1]" |
| ensure | content contains | "expected":"same" |
| ensure | content contains | drew 0+3 |
| ensure | content contains | drew [3+1, 0+3] |
| ensure | content contains | shrunk |
| ensure | source contains; | PropertySpanRow | span:0..5 length 1..4 |
| ensure | source contains; | PropertySpanRow | list of span:0..5 length 1..3 size 2..2 |
| reject | source contains; | PropertySpanRow | \| 0+1 \| |
| reject | source contains; | PropertySpanRow | [0+1, 0+1] |

| script | result view |
| --- | --- |
| show | rendered result |
