{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Property tables send string cells

The wiki draws the inputs and sends them as ordinary Slim strings. A Python server that only understands strings can run the property, and it shrinks the same way. `hello` answers `world`, so every word fails the expectation `red`. Seed 42 draws `blue` and shrinks to `red`.

| script | page driver |
| --- | --- |
| given page | PropertyPy | with content | &#96;&#96;&#96;agnesse<br>define COMMAND_PATTERN<br>python3 AgNesseRoot/files/agnesse/tiny_slim.py<br>&#96;&#96;&#96;<br><br>&#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| property:tiny \|<br>\| trials \| 1 \|<br>\| seed \| 42 \|<br>\| word \| hello? \|<br>\| one of:red,green,blue \| red \| |
| make | PropertyPy | a test page |
| check | request page | PropertyPy?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content contains | "actual":"red" |
| ensure | content contains | "actual":"world" |
| ensure | content contains | "expected":"red" |
| ensure | content contains | drew blue |
| ensure | content contains | shrunk |
| ensure | source contains; | PropertyPy | seed \| 42 |
| ensure | source contains; | PropertyPy | one of:red,green,blue |
| reject | source contains; | PropertyPy | drew blue |
