{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
`AgNesse` is started in the set up fixture so it's running,

| AgNesse Status |
| --- |
| isRunning? |
| true |

A Graceful shutdown is initiated by making an HTTP request with the responder=shutdown

| Response Requester. | |
| --- | --- |
| uri | valid? |
| ?responder=shutdown | true |

Wait a bit for things to settle down

| Sleep | 100 |
| --- | --- |

Now `AgNesse` is no longer running.

| AgNesse Status |
| --- |
| isRunning? |
| false |
