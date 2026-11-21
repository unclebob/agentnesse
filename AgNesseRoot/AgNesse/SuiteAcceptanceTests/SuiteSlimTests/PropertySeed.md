{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# The same seed draws the same row

Seed 4 shrinks to `[0]` on both runs. The page still says `4`. The result is where the counterexample appears.

````agnesse
define PropertyReplay
```agnesse
define SLIM_PORT
1
```

| property:agnesse.slim.test.ListReversal |
| trials | 1 |
| seed | 4 |
| items | still empty? |
| list of int:0..9 size 1..6 | true |
````

| script | page driver |
| --- | --- |
| given page | PropertyReplay | with content | ${PropertyReplay} |
| make | PropertyReplay | a test page |
| check | request page | PropertyReplay?test&format=json | 200 |
| ensure | content contains | "actual":"[0]" |
| ensure | content contains | drew [6, 5, 0, 9, 2] |
| ensure | content contains | shrunk |
| check | request page | PropertyReplay?test&format=json | 200 |
| ensure | content contains | "actual":"[0]" |
| ensure | source contains; | PropertyReplay | seed \| 4 |
| reject | source contains; | PropertyReplay | [0] |

| script | result view |
| --- | --- |
| show | rendered result |
