{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Python threads share one locked seat

The Python Slim server accepts one connection. A `lanes` instruction runs each branch on its own thread against that connection's seat. `safe` books under a lock, so one call returns `booked` and the other returns `full`. A pipe on port `1` carries the table.

```agnesse
define COMMAND_PATTERN
python3 AgNesseRoot/files/agnesse/tiny_slim.py
```

```agnesse
define SLIM_PORT
1
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
