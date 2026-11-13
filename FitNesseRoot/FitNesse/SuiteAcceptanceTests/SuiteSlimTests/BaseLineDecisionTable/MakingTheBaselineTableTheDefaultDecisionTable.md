{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
`SLIM_DT_BASELINE` makes an ordinary decision table behave as a baseline table, so a blank cell repeats the base scenario.

```agentnesse
define SLIM_DT_BASELINE
true
```

| import |
| --- |
| fitnesse.slim.test |


Given I have at least one milk remaining, I should not go to the store.

| should I buy milk | | | | |
| --- | --- | --- | --- | --- |
| cash in wallet | credit card | pints of milk remaining | go to store? | # |
| 0 | no | 1 | no | . |
| | | 2 | | . |
| | | 7 | | . |
| 10 | | | | . |
| | yes | | | . |
| 10 | yes | | | . |
| 1 | | 1 | | . |
