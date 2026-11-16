{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}

| script | echo fixture | | |
| --- | --- | --- | --- |
| show | echo | ${FITNESSE_VERSION} | |
| check not | echo | ${FITNESSE_VERSION} | vUnknown |
