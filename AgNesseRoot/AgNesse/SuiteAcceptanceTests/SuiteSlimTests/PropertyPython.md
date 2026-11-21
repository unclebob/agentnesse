{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Property tables send string cells

The wiki draws the inputs and sends them as ordinary Slim strings. A Python server that only understands strings can run the property, and it shrinks the same way. `hello` answers `world`, so every word fails the expectation `red`. Seed 42 draws `blue` and shrinks to `red`.

````agnesse
define PropertyPy
```agnesse
define COMMAND_PATTERN
python3 AgNesseRoot/files/agnesse/tiny_slim.py
```

```agnesse
define SLIM_PORT
1
```

| property:tiny |
| trials | 1 |
| seed | 42 |
| word | hello? |
| one of:red,green,blue | red |
````

| script | page driver |
| --- | --- |
| given page | PropertyPy | with content | ${PropertyPy} |
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

| script | result view |
| --- | --- |
| show | rendered result |
