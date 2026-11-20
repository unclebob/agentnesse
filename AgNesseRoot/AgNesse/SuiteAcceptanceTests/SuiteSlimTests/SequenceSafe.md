{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Two threads cannot both take the last seat

The fixture books under a lock. Each trial is a fresh seat. Two threads each call `book` once. One return is `booked` and the other is `full`, which is a serial order, so the trial passes. The seed stays on the result.

```agnesse
define SLIM_FLAGS
-d
```

```agnesse
define SLIM_PORT
0
```

| sequence:agnesse.slim.test.LastSeatSafe |
| --- |
| trials | 4 |
| threads | 2..2 |
| prefix | 0..0 |
| steps | 1..1 |
| seed | 1 |
| state | seats=1 |
| command | when | next | result? |
| book | seats=1 | seats=0 | booked |
| book | seats=0 | | full |
