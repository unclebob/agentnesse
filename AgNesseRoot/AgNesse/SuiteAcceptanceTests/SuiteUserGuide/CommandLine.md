{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Command line

`-c` runs one test or suite and writes JSON. The process exits with the number of failing pages. `-b` names the file. `--rerun-failures` points at a file of failed pages, or at a full results document. A run also writes `.agnesse/last-failures.json` beside the wiki root.

The JSON document has `"schemaVersion": 1`. A page that passed has `"status": "pass"`.

| script | command run |
| --- | --- |
| page; | CliMixed | suite | x |
| page; | CliMixed.CliPass | test | \| script \| agnesse.fixtures.EchoFixture \|<br>\| --- \| --- \| --- \|<br>\| check \| echo \| hello \| hello \| |
| page; | CliMixed.CliFail | test | \| script \| agnesse.fixtures.EchoFixture \|<br>\| --- \| --- \| --- \|<br>\| check \| echo \| hello \| goodbye \| |
| check | exit for | CliMixed.CliPass?test | 0 |
| ensure | output contains | "schemaVersion": 1 |
| ensure | output contains | "status": "pass" |
| check | exit for | CliMixed?suite | 1 |
| check | rerun failures | CliMixed?suite | 1 |
| reject | output contains | CliPass |
| ensure | output contains | CliFail |
