{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# A deadline shrinks to the boundary

`past?` is true at the clock plus 36 hours. The range is `now+35h59m` through `now+36h01m`. The clock is `2024-06-01T00:00:00Z`, so the deadline is `2024-06-02T12:00:00Z`. Seed 1 draws `2024-06-02T12:00:41Z`. Shrinking keeps the deadline. The saved page still has the generator.

````agnesse
define PropertyDue
```agnesse
define SLIM_PORT
1
```

```agnesse
define zone
UTC
```

| property:agnesse.slim.test.Deadline |
| trials | 1 |
| seed | 1 |
| now | 2024-06-01T00:00:00Z |
| now | at | past? |
| now | datetime:now+35h59m..now+36h01m | false |
````

| script | page driver |
| --- | --- |
| given page | PropertyDue | with content | ${PropertyDue} |
| make | PropertyDue | a test page |
| check | request page | PropertyDue?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content contains | "right":0 |
| ensure | content contains | "actual":"2024-06-02T12:00:00Z" |
| ensure | content contains | "actual":"true" |
| ensure | content contains | "expected":"false" |
| ensure | content contains | drew 2024-06-01T00:00:00Z, 2024-06-02T12:00:41Z |
| ensure | content contains | shrunk |
| ensure | source contains; | PropertyDue | datetime:now+35h59m..now+36h01m |
| reject | source contains; | PropertyDue | 2024-06-02T12:00:00Z |
| reject | source contains; | PropertyDue | 2024-06-02T12:00:41Z |

| script | result view |
| --- | --- |
| show | rendered result |
