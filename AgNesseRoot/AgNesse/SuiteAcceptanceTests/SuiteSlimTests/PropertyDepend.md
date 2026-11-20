{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Python reads a dependent pair and a record

The wiki draws the cells and sends them as strings. `endIsAfterStart` answers `yes` when the end is greater than the start, so a claim of `no` fails and shrinks to 0 and 1. The same claim of `yes` passes. `kind` reads the JSON object. The claim expects `other`, and the result is `id`.

| script | page driver |
| --- | --- |
| given page | PropertyPyDepend | with content | &#96;&#96;&#96;agnesse<br>define COMMAND_PATTERN<br>python3 AgNesseRoot/files/agnesse/tiny_slim.py<br>&#96;&#96;&#96;<br><br>&#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| script \| tiny \|<br><br>\| scenario \| order \| start \| stays after \| end \|<br>\| check \| end is after start; \| @start \| @end \| no \|<br><br>\| property \| order stays after \|<br>\| trials \| 1 \|<br>\| seed \| 1 \|<br>\| start \| int:0..5 \|<br>\| end \| int after start:0..6 \|<br><br>\| scenario \| order \| start \| stays above \| end \|<br>\| check \| end is after start; \| @start \| @end \| yes \|<br><br>\| property \| order stays above \|<br>\| trials \| 4 \|<br>\| seed \| 1 \|<br>\| start \| int:0..5 \|<br>\| end \| int after start:0..9 \|<br><br>\| scenario \| show kind \| person \|<br>\| check \| kind \| @person \| other \|<br><br>\| property \| show kind \|<br>\| trials \| 1 \|<br>\| seed \| 1 \|<br>\| person \| record:name one of:Ada; doc record:(kind one of:id) \| |
| make | PropertyPyDepend | a test page |
| check | request page | PropertyPyDepend?test&format=json | 200 |
| ensure | content contains | "wrong":2 |
| ensure | content matches | "right":1[,}] |
| ensure | content contains | "actual":"0" |
| ensure | content contains | "actual":"1" |
| ensure | content contains | "actual":"yes" |
| ensure | content contains | "expected":"no" |
| ensure | content contains | "actual":"id" |
| ensure | content contains | "expected":"other" |
| ensure | content contains | shrunk |
| ensure | source contains; | PropertyPyDepend | int after start:0..6 |
| ensure | source contains; | PropertyPyDepend | record:(kind one of:id) |
| reject | source contains; | PropertyPyDepend | \| 0 \| 1 \| |
