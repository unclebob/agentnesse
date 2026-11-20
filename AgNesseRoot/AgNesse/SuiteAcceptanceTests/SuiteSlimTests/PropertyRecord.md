{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# A record shrinks one field at a time

`record:n int:1..5` is a JSON object, and the int is a JSON string. The claim expects the word `same`, so the draw fails. Seed 1 draws `{"n":"2"}` and shrinks the field to `{"n":"1"}`.

| script | page driver |
| --- | --- |
| given page | PropertyPerson | with content | &#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| script \| agnesse.slim.test.TestSlim \|<br><br>\| scenario \| echo back \| value \|<br>\| check \| echo string \| @value \| same \|<br><br>\| property \| echo back \|<br>\| trials \| 1 \|<br>\| seed \| 1 \|<br>\| value \| record:n int:1..5 \| |
| make | PropertyPerson | a test page |
| check | request page | PropertyPerson?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content contains | "right":0 |
| ensure | content contains | {\\"n\\":\\"1\\"} |
| ensure | content contains | {\\"n\\":\\"2\\"} |
| ensure | content contains | "expected":"same" |
| ensure | content contains | shrunk |
| ensure | source contains; | PropertyPerson | record:n int:1..5 |
| reject | source contains; | PropertyPerson | {"n":"1"} |
