{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# A unique sorted list stays strictly increasing

`list of int:0..9 size 3..3 unique sorted` draws three distinct ints in order. The claim expects the word `same`, so the draw fails. Shrinking walks toward zero and keeps the list strictly increasing. Seed 1 draws `[0, 3, 8]` and ends at `[0, 1, 2]`.

| script | page driver |
| --- | --- |
| given page | PropertyIds | with content | &#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| script \| agnesse.slim.test.TestSlim \|<br><br>\| scenario \| echo back \| value \|<br>\| check \| echo string \| @value \| same \|<br><br>\| property \| echo back \|<br>\| trials \| 1 \|<br>\| seed \| 1 \|<br>\| value \| list of int:0..9 size 3..3 unique sorted \| |
| make | PropertyIds | a test page |
| check | request page | PropertyIds?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content contains | "right":0 |
| ensure | content contains | "actual":"[0, 1, 2]" |
| ensure | content contains | "expected":"same" |
| ensure | content contains | drew [0, 3, 8] |
| ensure | content contains | shrunk |
| ensure | source contains; | PropertyIds | list of int:0..9 size 3..3 unique sorted |
| reject | source contains; | PropertyIds | [0, 1, 2] |
