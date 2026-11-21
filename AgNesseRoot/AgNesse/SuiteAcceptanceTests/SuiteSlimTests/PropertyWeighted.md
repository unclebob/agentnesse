{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# A heavy choice shrinks toward the first

`one of:red 1, blue 1, green 98` still draws the rare words. The claim expects `red`. Seed 1 draws `green` and shrinks to `blue`, the earlier choice that still fails. The weight does not pull the shrink toward the common word.

````agnesse
define PropertyWeights
```agnesse
define SLIM_PORT
1
```

| script | agnesse.slim.test.TestSlim |

| scenario | echo back | word |
| check | echo string | @word | red |

| property | echo back |
| trials | 1 |
| seed | 1 |
| word | one of:red 1, blue 1, green 98 |
````

| script | page driver |
| --- | --- |
| given page | PropertyWeights | with content | ${PropertyWeights} |
| make | PropertyWeights | a test page |
| check | request page | PropertyWeights?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content contains | "right":0 |
| ensure | content contains | "actual":"blue" |
| ensure | content contains | "expected":"red" |
| ensure | content contains | drew green |
| ensure | content contains | shrunk |
| ensure | source contains; | PropertyWeights | one of:red 1, blue 1, green 98 |
| reject | source contains; | PropertyWeights | drew green |

| script | result view |
| --- | --- |
| show | rendered result |
