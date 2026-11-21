{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# A unique sorted list stays strictly increasing

`list of int:0..9 size 3..3 unique sorted` draws three distinct ints in order. The claim expects the word `same`, so the draw fails. Shrinking walks toward zero and keeps the list strictly increasing. Seed 1 draws `[0, 3, 8]` and ends at `[0, 1, 2]`.

````agnesse
define PropertyIds
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
| value | list of int:0..9 size 3..3 unique sorted |
````

| script | page driver |
| --- | --- |
| given page | PropertyIds | with content | ${PropertyIds} |
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

| script | result view |
| --- | --- |
| show | rendered result |
