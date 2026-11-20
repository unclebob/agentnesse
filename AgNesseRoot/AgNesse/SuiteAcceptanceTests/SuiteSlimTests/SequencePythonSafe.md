{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Python threads share one locked seat

The Python Slim server accepts one socket per thread and those sockets share the seat. `safe` books under a lock, so one call returns `booked` and the other returns `full`.

```agnesse
define COMMAND_PATTERN
python3 AgNesseRoot/files/agnesse/tiny_slim.py
```

```agnesse
define SLIM_PORT
0
```

| sequence:tiny | safe |
| --- | --- |
| trials | 4 |
| threads | 2..2 |
| prefix | 0..0 |
| steps | 1..1 |
| seed | 1 |
| state | seats=1 |
| command | when | next | result? |
| book | seats=1 | seats=0 | booked |
| book | seats=0 | | full |
