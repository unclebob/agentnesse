{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# A hostile string survives the round trip

`string:adversarial` mixes ordinary text with a fixed catalog. Seed 43 draws a script tag. The claim expects `nope`, so every draw fails, and shrinking walks to the empty string. The trials cell names the original draw, so the payload came back through Slim. The result page escapes that cell; the JSON actual below is the raw label.

| script | page driver |
| --- | --- |
| given page | PropertyHostileDraw | with content | &#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| script \| agnesse.slim.test.TestSlim \|<br><br>\| scenario \| echo back \| word \|<br>\| check \| echo string \| @word \| nope \|<br><br>\| property \| echo back \|<br>\| trials \| 1 \|<br>\| seed \| 43 \|<br>\| word \| string:adversarial length 0..40 \| |
| make | PropertyHostileDraw | a test page |
| check | request page | PropertyHostileDraw?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content contains | "right":0 |
| ensure | content contains | "expected":"nope" |
| ensure | content contains | alert(1) |
| ensure | content contains | shrunk |
| ensure | source contains; | PropertyHostileDraw | string:adversarial length 0..40 |
| reject | source contains; | PropertyHostileDraw | alert(1) |
