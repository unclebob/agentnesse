{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# A bad property

`still empty?` is false for a list of two or three ints. The first draw for seed 11 is `[4, 0]`, and that row is what the result shows. Shrinking would have moved `4` toward `0`. The saved page still has the generator, not the counterexample.

| script | page driver |
| --- | --- |
| given page | PropertyFail | with content | &#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| property:agnesse.slim.test.ListReversal \|<br>\| trials \| 1 \|<br>\| seed \| 11 \|<br>\| items \| still empty? \|<br>\| list of int:0..4 size 2..3 \| true \| |
| make | PropertyFail | a test page |
| check | request page | PropertyFail?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content contains | "right":0 |
| ensure | content contains | "actual":"[4, 0]" |
| ensure | content contains | "actual":"false" |
| ensure | content contains | "expected":"true" |
| ensure | content contains | "status":"fail" |
| ensure | source contains; | PropertyFail | seed \| 11 |
| reject | source contains; | PropertyFail | [4, 0] |
