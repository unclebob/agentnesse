{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Python receives a money cell as text

The wiki draws the decimal and sends it as a Slim string. `echo` on the Python server returns that string. The check expects `other`, so the result records the returned text `1.50`.

````agnesse
define PropertyMoneyPy
```agnesse
define COMMAND_PATTERN
python3 AgNesseRoot/files/agnesse/tiny_slim.py
```

```agnesse
define SLIM_PORT
1
```

| script | tiny |

| scenario | show price | price |
| check | echo | @price | other |

| property | show price |
| trials | 1 |
| seed | 1 |
| price | money:1.50..1.50 |
````

| script | page driver |
| --- | --- |
| given page | PropertyMoneyPy | with content | ${PropertyMoneyPy} |
| make | PropertyMoneyPy | a test page |
| check | request page | PropertyMoneyPy?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content contains | "right":0 |
| ensure | content contains | "actual":"1.50" |
| ensure | content contains | "expected":"other" |
| ensure | source contains; | PropertyMoneyPy | money:1.50..1.50 |
| reject | source contains; | PropertyMoneyPy | drew |

| script | result view |
| --- | --- |
| show | rendered result |
