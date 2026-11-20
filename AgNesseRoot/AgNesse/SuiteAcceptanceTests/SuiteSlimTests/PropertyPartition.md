{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# A partition keeps its total

`partition:10 into 3` draws three non-negative ints that add to 10. The claim expects the word `same`, so the draw fails. Shrinking moves the later parts onto the first. Seed 1 draws `[2, 7, 1]` and ends at `[10, 0, 0]`.

| script | page driver |
| --- | --- |
| given page | PropertyParts | with content | &#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| script \| agnesse.slim.test.TestSlim \|<br><br>\| scenario \| echo back \| value \|<br>\| check \| echo string \| @value \| same \|<br><br>\| property \| echo back \|<br>\| trials \| 1 \|<br>\| seed \| 1 \|<br>\| value \| partition:10 into 3 \| |
| make | PropertyParts | a test page |
| check | request page | PropertyParts?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content contains | "right":0 |
| ensure | content contains | "actual":"[10, 0, 0]" |
| ensure | content contains | "expected":"same" |
| ensure | content contains | drew [2, 7, 1] |
| ensure | content contains | shrunk |
| ensure | source contains; | PropertyParts | partition:10 into 3 |
| reject | source contains; | PropertyParts | [10, 0, 0] |
