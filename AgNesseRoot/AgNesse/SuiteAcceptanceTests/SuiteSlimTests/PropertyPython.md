{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Property tables send string cells

The wiki draws the inputs and sends them as ordinary Slim strings. A Python server that only understands strings can run the property. `hello` answers `world`. The generated word is just another string cell.

```agnesse
define COMMAND_PATTERN
python3 AgNesseRoot/files/agnesse/tiny_slim.py
```

```agnesse
define SLIM_PORT
1
```

| property:tiny |
| --- |
| trials | 4 |
| seed | 5 |
| word | hello? |
| one of:red,green,blue | world |
