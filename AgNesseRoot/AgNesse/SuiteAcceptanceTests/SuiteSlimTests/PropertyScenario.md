{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# A scenario property shows the shrunk steps

The property calls `echo back`. Seed 6 draws `blue`, which is not `nope`, and shrinking keeps `red`. The result has one failing check, and that check is the scenario's steps for `red`, rendered as a child table.

````agnesse
define PropertyChild
```agnesse
define SLIM_PORT
1
```

| script | agnesse.slim.test.TestSlim |

| scenario | echo back | word |
| check | echo string | @word | nope |

| property | echo back |
| trials | 1 |
| seed | 6 |
| word | one of:red,blue |
````

| script | page driver |
| --- | --- |
| given page | PropertyChild | with content | ${PropertyChild} |
| make | PropertyChild | a test page |
| check | request page | PropertyChild?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content contains | "right":0 |
| ensure | content contains | drew blue |
| ensure | content contains | shrunk |
| ensure | content contains | "expected":"nope" |
| ensure | content contains | "actual":"red" |
| ensure | content contains | "status":"fail" |
| ensure | source contains; | PropertyChild | seed \| 6 |
| reject | source contains; | PropertyChild | drew blue |
| reject | content contains | "wrong":2 |

| script | result view |
| --- | --- |
| show | rendered result |
