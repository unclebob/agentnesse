{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# An identifier shrinks toward the first character

`pattern:ID-[0-9]{2}` draws `ID-` and two digits. The claim expects `ID-00`. Seed 1 draws `ID-58`. Shrinking walks toward `0` and stops at `ID-01`, the first candidate on that walk that still fails.

````agnesse
define PropertyId
```agnesse
define SLIM_PORT
1
```

| script | agnesse.slim.test.TestSlim |

| scenario | echo back | word |
| check | echo string | @word | ID-00 |

| property | echo back |
| trials | 1 |
| seed | 1 |
| word | pattern:ID-[0-9]{2} |
````

| script | page driver |
| --- | --- |
| given page | PropertyId | with content | ${PropertyId} |
| make | PropertyId | a test page |
| check | request page | PropertyId?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content contains | "right":0 |
| ensure | content contains | "actual":"ID-01" |
| ensure | content contains | "expected":"ID-00" |
| ensure | content contains | drew ID-58 |
| ensure | content contains | shrunk |
| ensure | source contains; | PropertyId | pattern:ID-[0-9]{2} |
| reject | source contains; | PropertyId | ID-58 |
| reject | source contains; | PropertyId | ID-01 |

| script | result view |
| --- | --- |
| show | rendered result |
