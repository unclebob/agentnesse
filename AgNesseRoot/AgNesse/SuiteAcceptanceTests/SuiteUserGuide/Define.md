{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Define

A `define` block sets a variable. `${GREETING}` in a Slim cell is replaced before the fixture sees the cell.

```agnesse
define GREETING
hello
```

| script | echo fixture |
| --- | --- |
| check | echo | ${GREETING} | hello |
