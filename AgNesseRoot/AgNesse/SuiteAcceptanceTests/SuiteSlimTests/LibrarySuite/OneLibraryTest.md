{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}

| Library |
| --- |
| echo support |


| script | library fixture |
| --- | --- |


| script |
| --- |
| given installed library echo support |
| when a method not present on the fixture is called |
| then it should be called on the installed library |

