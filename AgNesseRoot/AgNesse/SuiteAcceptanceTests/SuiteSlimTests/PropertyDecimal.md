{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# A price shrinks toward a dollar

`affordable?` is true when the price is under one dollar. `money:0..5` is a fixed-point decimal at scale 2. Seed 1 draws 2.43, and that fails. The row on the result is 1.00, the smallest round price that still fails.

| script | page driver |
| --- | --- |
| given page | PropertyAmount | with content | &#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| property:agnesse.slim.test.Amount \|<br>\| trials \| 1 \|<br>\| seed \| 1 \|<br>\| price \| affordable? \|<br>\| money:0..5 \| true \| |
| make | PropertyAmount | a test page |
| check | request page | PropertyAmount?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content contains | "right":0 |
| ensure | content contains | "actual":"1.00" |
| ensure | content contains | "actual":"false" |
| ensure | content contains | "expected":"true" |
| ensure | content contains | drew 2.43 |
| ensure | content contains | shrunk |
| ensure | source contains; | PropertyAmount | money:0..5 |
| reject | source contains; | PropertyAmount | 2.43 |
| reject | source contains; | PropertyAmount | \| 1.00 \| |
