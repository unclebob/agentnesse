{
  "type": "test",
  "tags": ["testSuite"],
  "help": null,
  "prune": false
}

| script | agnesse.slim.test.TestSlim | | |
| --- | --- | --- | --- |
| $VALUE= | echo string | Bob | |
| $VALUEX= | echo string | bill | |


| script | | | |
| --- | --- | --- | --- |
| check | echo string | $VALUE | Bob |
| check | echo string | $VALUEX | bill |
| check | echo string | $VALUE $VALUEX | Bob bill |
