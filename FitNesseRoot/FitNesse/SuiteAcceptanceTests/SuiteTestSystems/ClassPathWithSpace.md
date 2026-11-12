{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
### A space in the classpath must still allow to start the test system
```agentnesse
path
abc xyz
```

### A trailing space in classpath definition  must still allow to start the test system
```agentnesse
define TS
trailingspace
```
```agentnesse
path
${TS}
```
<!-- !path trailingspace -->


| script | | | |
| --- | --- | --- | --- |
| check | echo | pass | pass |
