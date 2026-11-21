{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# A meter shrinks without dropping its total

`monotonic:0..100 size 4..4 reset` may fall, and the running total counts each rise from the floor. Seed 1 draws `[89, 44, 52, 80]`, which totals 169, and shrinks to `[100, 0, 69, 69]`. A list without `reset` only rises, and `monotonic:0..20 size 2..4` shrinks to `[0, 0]`. Both claims expect `same`, so both fail.

````agnesse
define PropertyRise
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
| value | monotonic:0..100 size 4..4 reset |

| property | echo back |
| trials | 1 |
| seed | 1 |
| value | monotonic:0..20 size 2..4 |
````

| script | page driver |
| --- | --- |
| given page | PropertyRise | with content | ${PropertyRise} |
| make | PropertyRise | a test page |
| check | request page | PropertyRise?test&format=json | 200 |
| ensure | content contains | "wrong":2 |
| ensure | content contains | "right":0 |
| ensure | content contains | "actual":"[100, 0, 69, 69]" |
| ensure | content contains | "actual":"[0, 0]" |
| ensure | content contains | drew [89, 44, 52, 80] |
| ensure | content contains | drew [6, 9] |
| ensure | content contains | shrunk |
| reject | content contains | shrink cap |
| ensure | source contains; | PropertyRise | monotonic:0..100 size 4..4 reset |
| ensure | source contains; | PropertyRise | monotonic:0..20 size 2..4 |
| reject | source contains; | PropertyRise | [100, 0, 69, 69] |
| reject | source contains; | PropertyRise | [89, 44, 52, 80] |

| script | result view |
| --- | --- |
| show | rendered result |
