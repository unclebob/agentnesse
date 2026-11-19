{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Property

A property table draws the inputs. It checks one claim against many generated values, instead of a list of examples you type by hand. Name a fixture the way a decision table does. Optional `trials` and `seed` rows come next, then a column header and one generator row. An output column ends in `?` or `!` and its generator cell is the expected value.

Reversing a list twice returns the same list. Five trials, one assertion.

| property:agnesse.slim.test.ListReversal |
| --- |
| trials | 5 |
| seed | |
| items | reversed twice? |
| list of int:0..3 size 0..2 | true |

One property is one assertion, however many trials run. A pass colors the trials cell (`5 trials`). The default is 100 trials when that row is omitted or left blank.

The seed is shown on the result and is not written back onto the page. Leave the cell blank and the result fills it in. Copy that number into the cell to draw the same inputs again.

| script | page driver |
| --- | --- |
| given page | PropertyPass | with content | &#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| property:agnesse.slim.test.ListReversal \|<br>\| trials \| 5 \|<br>\| seed \| \|<br>\| items \| reversed twice? \|<br>\| list of int:0..3 size 0..2 \| true \| |
| make | PropertyPass | a test page |
| check | request page | PropertyPass?test&format=json | 200 |
| ensure | content matches | "right":1[,}] |
| ensure | content contains | "wrong":0 |
| ensure | content matches | "actual":"-?[0-9]+" |
| ensure | source contains; | PropertyPass | \| seed \| \| |
| reject | content contains | "status":"fail" |

## Generators

Generator cells are lowercase. Whitespace may separate the tokens. These are the v1 forms. Ranges are inclusive.

`int` draws from `-10000..10000`. A filter may narrow it, written before an optional range: `!=`, `>=`, `<=`, `>`, or `<`. `int < 50:0..40` is every int from 0 through 40. A filter that leaves no int is rejected, so a draw never has to be discarded.

`boolean` draws `true` or `false`.

`string` draws printable ASCII except `|`, so the text can sit in a Markdown cell. The length default is `0..20`. `string:ascii` is that same alphabet. `length 0..4` sets the length of the string just named.

`list of` wraps any generator, including another list. The size default is `0..8`. `size 0..2` sets the size of the list just closed.

`one of:red,green,blue` draws one of those words. A choice ends at a comma or at whitespace.

`below?` is true for every int under 50, so this filtered range passes.

| property:agnesse.slim.test.Bound |
| --- |
| trials | 5 |
| seed | 1 |
| n | below? |
| int < 50:0..40 | true |

A scenario can be the claim. `@value` is the drawn cell, and the check expects that same text, so each of these passes.

| script | agnesse.slim.test.TestSlim |
| --- |

| scenario | round trip | value |
| --- | --- | --- |
| check | echo string | @value | @value |

| property | round trip |
| --- |
| trials | 2 |
| seed | 1 |
| value | int |

| property | round trip |
| --- |
| trials | 3 |
| seed | 2 |
| value | boolean |

| property | round trip |
| --- |
| trials | 3 |
| seed | 3 |
| value | string length 0..4 |

| property | round trip |
| --- |
| trials | 2 |
| seed | 4 |
| value | string:ascii length 1..3 |

| property | round trip |
| --- |
| trials | 3 |
| seed | 5 |
| value | list of int:0..3 size 0..2 |

| property | round trip |
| --- |
| trials | 3 |
| seed | 6 |
| value | one of:red,green,blue |

The value in each input cell is an ordinary string. A Slim server that is not Java sees that same text. Here a Python process answers `world` from `hello`, and the generated word is just another string cell.

| script | page driver |
| --- | --- |
| given page | PropertyGuidePy | with content | &#96;&#96;&#96;agnesse<br>define COMMAND_PATTERN<br>python3 AgNesseRoot/files/agnesse/tiny_slim.py<br>&#96;&#96;&#96;<br><br>&#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| property:tiny \|<br>\| trials \| 2 \|<br>\| seed \| 1 \|<br>\| word \| hello? \|<br>\| one of:red,green \| world \| |
| make | PropertyGuidePy | a test page |
| check | request page | PropertyGuidePy?test&format=json | 200 |
| ensure | content matches | "right":1[,}] |
| ensure | content contains | "wrong":0 |
| ensure | content contains | "actual":"2 trials" |
| ensure | source contains; | PropertyGuidePy | one of:red,green |

## Shrinking

After the first failing trial, shrinking sends simpler candidates. Each round is one Slim batch: every simpler value of one input, most aggressive first. The first candidate in that batch that still fails is kept. The page shows one row, the shrunk one, and the trials cell names the original draw, as in `drew [6, 5, 0, 9, 2]; shrunk`. The same seed shrinks to the same row.

The walk stops after 32 accepted steps. The trials cell then says `shrink cap`, and the row is the value reached then, which may not be the smallest failure.

A fixture exception counts as a failing candidate, so the walk continues, and that exception is not an extra assertion. A stop-test exception shows the current candidate and sends no further batch.

When the property calls a scenario, a failure renders that scenario's steps for the shrunk input as a child table, the same way a scenario call already does. The round-trip tables above pass, so they have no child table.
