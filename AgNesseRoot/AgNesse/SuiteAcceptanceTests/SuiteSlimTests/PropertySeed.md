{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# The same seed draws the same row

Seed 11 draws `[4, 0]` again on the second run. The page still says `11`. The result is where the counterexample appears.

| script | page driver |
| --- | --- |
| given page | PropertyReplay | with content | &#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| property:agnesse.slim.test.ListReversal \|<br>\| trials \| 1 \|<br>\| seed \| 11 \|<br>\| items \| still empty? \|<br>\| list of int:0..4 size 2..3 \| true \| |
| make | PropertyReplay | a test page |
| check | request page | PropertyReplay?test&format=json | 200 |
| ensure | content contains | "actual":"[4, 0]" |
| check | request page | PropertyReplay?test&format=json | 200 |
| ensure | content contains | "actual":"[4, 0]" |
| ensure | source contains; | PropertyReplay | seed \| 11 |
| reject | source contains; | PropertyReplay | [4, 0] |
