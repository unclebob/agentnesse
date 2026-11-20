{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# An end stays after its start

`int after start` is at least one above `start`. The claim expects the word `same`, so the drawn pair fails. Seed 1 shrinks to 0 and 1, and 1 is still after 0. A reference to a later column, and a column that names itself, are refused before any trial runs.

| script | page driver |
| --- | --- |
| given page | PropertyEnds | with content | &#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| script \| agnesse.slim.test.TestSlim \|<br><br>\| scenario \| order \| start \| stays after \| end \|<br>\| check \| echo string \| @end \| same \|<br><br>\| property \| order stays after \|<br>\| trials \| 1 \|<br>\| seed \| 1 \|<br>\| start \| int:0..5 \|<br>\| end \| int after start:0..6 \| |
| make | PropertyEnds | a test page |
| check | request page | PropertyEnds?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content contains | "right":0 |
| ensure | content contains | "actual":"0" |
| ensure | content contains | "actual":"1" |
| ensure | content contains | "expected":"same" |
| ensure | content contains | shrunk |
| ensure | source contains; | PropertyEnds | int after start:0..6 |
| ensure | source contains; | PropertyEnds | seed \| 1 |
| reject | source contains; | PropertyEnds | \| 0 \| 1 \| |

| script | page driver |
| --- | --- |
| given page | PropertyLater | with content | &#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| property:agnesse.slim.test.TestSlim \|<br>\| end \| start \| return int? \|<br>\| int after start \| int:0..3 \| 7 \|<br><br>\| property:agnesse.slim.test.TestSlim \|<br>\| end \| return int? \|<br>\| int after end \| 7 \| |
| make | PropertyLater | a test page |
| check | request page | PropertyLater?test&format=json | 200 |
| ensure | content contains | "wrong":0 |
| ensure | content contains | 2 exceptions |
| ensure | content contains | refers to later column |
| ensure | content contains | cycle in column |
| ensure | source contains; | PropertyLater | int after start |
| ensure | source contains; | PropertyLater | int after end |
