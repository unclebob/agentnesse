{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# A scenario property shows the shrunk steps

The property calls `echo back`. Seed 6 draws `blue`, which is not `nope`, and shrinking keeps `red`. The result has one failing check, and that check is the scenario's steps for `red`, rendered as a child table.

| script | page driver |
| --- | --- |
| given page | PropertyChild | with content | &#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| script \| agnesse.slim.test.TestSlim \|<br><br>\| scenario \| echo back \| word \|<br>\| check \| echo string \| @word \| nope \|<br><br>\| property \| echo back \|<br>\| trials \| 1 \|<br>\| seed \| 6 \|<br>\| word \| one of:red,blue \| |
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
