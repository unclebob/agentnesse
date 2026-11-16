{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}

| import |
| --- |
| agnesse.slim.test |


| script | test slim | |
| --- | --- | --- |
| $X= | echo int | 100 |


| script | test slim | $X |
| --- | --- | --- |
| check | returnConstructorArg | 100 |


| script: test slim | $X | |
| --- | --- | --- |
| check | returnConstructorArg | 100 |
| start | test slim | $X |
| check | returnConstructorArg | 100 |
