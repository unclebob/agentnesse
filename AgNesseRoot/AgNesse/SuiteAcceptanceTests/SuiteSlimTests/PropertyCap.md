{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# The shrink cap is reported

`below?` fails for every integer in this span, and the only smaller value still inside the span is one step down. The walk stops after 32 accepted steps. The trials cell says `shrink cap` and the row is the value reached then, not a fully minimal one.

| script | page driver |
| --- | --- |
| given page | PropertyCapped | with content | &#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| property:agnesse.slim.test.Bound \|<br>\| trials \| 1 \|<br>\| seed \| 1 \|<br>\| n \| below? \|<br>\| int:200..399 \| true \| |
| make | PropertyCapped | a test page |
| check | request page | PropertyCapped?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content contains | drew 396 |
| ensure | content contains | shrink cap |
| ensure | content contains | "actual":"364" |
| ensure | source contains; | PropertyCapped | seed \| 1 |
| reject | source contains; | PropertyCapped | drew 396 |
