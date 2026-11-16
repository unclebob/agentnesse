{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}

| script | echo fixture | | |
| --- | --- | --- | --- |
| show | echo | ${AGNESSE_VERSION} | |
| check not | echo | ${AGNESSE_VERSION} | vUnknown |
