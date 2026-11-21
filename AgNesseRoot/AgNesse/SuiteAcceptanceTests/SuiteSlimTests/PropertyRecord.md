{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# A record shrinks one field at a time

`record:n int:1..5` is a JSON object, and the int is a JSON string. The claim expects the word `same`, so the draw fails. Seed 1 draws `{"n":"2"}` and shrinks the field to `{"n":"1"}`.

````agnesse
define PropertyPerson
```agnesse
define SLIM_PORT
1
```

| script | agnesse.slim.test.TestSlim |

| scenario | echo back | value |
| check | echo string | @value | same |

| property | echo back |
| trials | 1 |
| seed | 1 |
| value | record:n int:1..5 |
````

| script | page driver |
| --- | --- |
| given page | PropertyPerson | with content | ${PropertyPerson} |
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

| script | result view |
| --- | --- |
| show | rendered result |
