{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# An int shrinks to the smallest failure

`below?` is true when the integer is less than 50. Seed 4 first draws 112, and that fails. The row on the result is 50, the smallest integer that still fails. The trials cell notes the draw.

| script | page driver |
| --- | --- |
| given page | PropertyBound | with content | &#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| property:agnesse.slim.test.Bound \|<br>\| trials \| 30 \|<br>\| seed \| 4 \|<br>\| n \| below? \|<br>\| int:0..200 \| true \| |
| make | PropertyBound | a test page |
| check | request page | PropertyBound?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content contains | "right":0 |
| ensure | content contains | "actual":"50" |
| ensure | content contains | drew 112 |
| ensure | content contains | shrunk |
| ensure | source contains; | PropertyBound | seed \| 4 |
| reject | source contains; | PropertyBound | drew 112 |
| reject | source contains; | PropertyBound | \| 50 \| |
