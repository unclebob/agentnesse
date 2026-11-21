{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# A bad property

`still empty?` is false for any list that has an element. Seed 4 draws `[6, 5, 0, 9, 2]`. Shrinking keeps the shortest list of small elements that still fails, `[0]`. The trials cell notes the original draw. The saved page still has the generator, not the counterexample.

````agnesse
define PropertyFail
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
| given page | PropertyFail | with content | ${PropertyFail} |
| make | PropertyFail | a test page |
| check | request page | PropertyFail?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content contains | "right":0 |
| ensure | content contains | "actual":"[0]" |
| ensure | content contains | drew [6, 5, 0, 9, 2] |
| ensure | content contains | shrunk |
| ensure | content contains | "actual":"false" |
| ensure | content contains | "expected":"true" |
| ensure | content contains | "status":"fail" |
| ensure | source contains; | PropertyFail | seed \| 4 |
| reject | source contains; | PropertyFail | [0] |
| reject | source contains; | PropertyFail | [6, 5, 0, 9, 2] |

| script | result view |
| --- | --- |
| show | rendered result |
