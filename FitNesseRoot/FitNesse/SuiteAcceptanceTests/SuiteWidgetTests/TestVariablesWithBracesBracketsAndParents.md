{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
```agentnesse
define BRACE
{
```
```agentnesse
define BRACKET
[
```
```agentnesse
define PAREN
(
```


| script | | | |
| --- | --- | --- | --- |
| check | echo | ${BRACE} ${BRACKET} ${PAREN} | { [ ( |
