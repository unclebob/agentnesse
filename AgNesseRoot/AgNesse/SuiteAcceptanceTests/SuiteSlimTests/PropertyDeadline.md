{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# A deadline shrinks to the boundary

`past?` is true at the clock plus 36 hours. The range is `now+35h59m` through `now+36h01m`. The clock is `2024-06-01T00:00:00Z`, so the deadline is `2024-06-02T12:00:00Z`. Seed 1 draws `2024-06-02T12:00:41Z`. Shrinking keeps the deadline. The saved page still has the generator.

| script | page driver |
| --- | --- |
| given page | PropertyDue | with content | &#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>&#96;&#96;&#96;agnesse<br>define zone<br>UTC<br>&#96;&#96;&#96;<br><br>\| property:agnesse.slim.test.Deadline \|<br>\| trials \| 1 \|<br>\| seed \| 1 \|<br>\| now \| 2024-06-01T00:00:00Z \|<br>\| now \| at \| past? \|<br>\| now \| datetime:now+35h59m..now+36h01m \| false \| |
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
