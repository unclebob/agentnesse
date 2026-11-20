{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Two threads cannot both take the last seat

The fixture books under a lock. Each trial is a fresh seat. One connection carries both threads: the table sends one `lanes` instruction, and the Java server runs each lane on its own thread. One return is `booked` and the other is `full`, which is a serial order, so the trial passes. The seed stays on the result. The suite pipe is enough; there is no extra socket and no `-d` flag.

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
