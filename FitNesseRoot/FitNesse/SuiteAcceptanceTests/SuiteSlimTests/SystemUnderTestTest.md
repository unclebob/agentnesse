{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}

| Import |
| --- |
| agnesse.slim.test |



| script | MySystemUnderTestDriver |
| --- | --- |
| foo | |
| ensure | driver called |
| reject | system under test called |


| script | MySystemUnderTestDriver |
| --- | --- |
| bar | |
| reject | driver called |
| ensure | system under test called |
