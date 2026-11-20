{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# A Go server speaks Slim

`go run` starts the server. One connection carries the whole table, including a pipe on port `1`. A `lanes` instruction runs each branch on its own goroutine.

```agnesse
define COMMAND_PATTERN
go -C slim/go run .
```

```agnesse
define SLIM_PORT
1
```

| slim |
| --- |
| word | echo? |
| red | red |

| property:slim |
| --- |
| trials | 1 |
| seed | 1 |
| word | hello? |
| one of:red | world |

| sequence:slim |
| --- |
| trials | 4 |
| steps | 1..3 |
| seed | 1 |
| state | phase=new |
| command | when | next | result? | phase? |
| open | phase=new | phase=open | ok | open |
| block | phase=open | phase=blocked | ok | blocked |
| release | phase=open | phase=released | ok | released |
| release | phase=blocked | | refused | blocked |

| sequence:slim | safe |
| --- |
| trials | 2 |
| threads | 2..2 |
| prefix | 0..0 |
| steps | 1..1 |
| seed | 1 |
| state | seats=1 |
| command | when | next | result? |
| book | seats=1 | seats=0 | booked |
| book | seats=0 | | full |
