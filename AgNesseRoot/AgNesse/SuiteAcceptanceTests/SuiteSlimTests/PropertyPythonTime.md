{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Python receives a datetime as text

The wiki draws the clock and the instant and sends each as a Slim string. `echo` returns that string. The clock check expects the text the `now` column drew. The instant check expects `other`, so the result records `2024-03-10T07:00:00Z`. A blank `now` on that second table is filled from the seed and is not written back.

| script | page driver |
| --- | --- |
| given page | PropertyTimePy | with content | &#96;&#96;&#96;agnesse<br>define COMMAND_PATTERN<br>python3 AgNesseRoot/files/agnesse/tiny_slim.py<br>&#96;&#96;&#96;<br><br>&#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>&#96;&#96;&#96;agnesse<br>define zone<br>UTC<br>&#96;&#96;&#96;<br><br>\| script \| tiny \|<br><br>\| scenario \| show clock \| clock \|<br>\| check \| echo \| @clock \| 2024-03-10T06:00:00Z \|<br><br>\| property \| show clock \|<br>\| trials \| 1 \|<br>\| seed \| 1 \|<br>\| now \| 2024-03-10T06:00:00Z \|<br>\| clock \| now \|<br><br>\| scenario \| show instant \| instant \|<br>\| check \| echo \| @instant \| other \|<br><br>\| property \| show instant \|<br>\| trials \| 1 \|<br>\| seed \| 1 \|<br>\| instant \| datetime:2024-03-10T07:00:00Z..2024-03-10T07:00:00Z \| |
| make | PropertyTimePy | a test page |
| check | request page | PropertyTimePy?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content matches | "right":1[,}] |
| ensure | content contains | "actual":"2024-03-10T07:00:00Z" |
| ensure | content contains | "actual":"2024-03-10T06:00:00Z" |
| ensure | content contains | "expected":"other" |
| ensure | content contains | 2000-01-01T00:00:01Z |
| ensure | source contains; | PropertyTimePy | datetime:2024-03-10T07:00:00Z..2024-03-10T07:00:00Z |
| reject | source contains; | PropertyTimePy | 2000-01-01T00:00:01Z |
| reject | source contains; | PropertyTimePy | drew |
