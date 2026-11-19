{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Python receives a money cell as text

The wiki draws the decimal and sends it as a Slim string. `echo` on the Python server returns that string. The check expects `other`, so the result records the returned text `1.50`.

| script | page driver |
| --- | --- |
| given page | PropertyMoneyPy | with content | &#96;&#96;&#96;agnesse<br>define COMMAND_PATTERN<br>python3 AgNesseRoot/files/agnesse/tiny_slim.py<br>&#96;&#96;&#96;<br><br>&#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| script \| tiny \|<br><br>\| scenario \| show price \| price \|<br>\| check \| echo \| @price \| other \|<br><br>\| property \| show price \|<br>\| trials \| 1 \|<br>\| seed \| 1 \|<br>\| price \| money:1.50..1.50 \| |
| make | PropertyMoneyPy | a test page |
| check | request page | PropertyMoneyPy?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content contains | "right":0 |
| ensure | content contains | "actual":"1.50" |
| ensure | content contains | "expected":"other" |
| ensure | source contains; | PropertyMoneyPy | money:1.50..1.50 |
| reject | source contains; | PropertyMoneyPy | drew |
