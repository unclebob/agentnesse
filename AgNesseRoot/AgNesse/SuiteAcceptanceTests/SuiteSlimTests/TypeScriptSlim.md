{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# The slim/ts server speaks Slim

`node` starts the server. One connection carries the whole table, including a pipe on port `1`. A `lanes` instruction runs every branch as an async task on that process.

This server has no real threads. The lanes share Node's one event loop and run concurrently by yielding at `await`, which is the closest honest equivalent of a thread. A racy book awaits a timer, so two lanes can both see the seat free. A safe book waits its turn on a promise chain. The server does not start worker threads.

```agnesse
define COMMAND_PATTERN
node --experimental-strip-types slim/ts/slim.ts
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
