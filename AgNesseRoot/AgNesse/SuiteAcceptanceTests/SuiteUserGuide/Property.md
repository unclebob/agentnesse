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

Generator cells are lowercase. Whitespace may separate the tokens. Ranges are inclusive.

`int` draws from `-10000..10000`. A filter may narrow it, written before an optional range: `!=`, `>=`, `<=`, `>`, or `<`. `int < 50:0..40` is every int from 0 through 40. A filter that leaves no int is rejected, so a draw never has to be discarded.

`boolean` draws `true` or `false`.

`string` draws printable ASCII except `|`, so the text can sit in a Markdown cell. The length default is `0..20`. `string:ascii` is that same alphabet. `length 0..4` sets the length of the string just named.

`list of` wraps any generator, including another list. The size default is `0..8`. `size 0..2` sets the size of the list just closed.

`one of:red,green,blue` draws one of those words. A choice ends at a comma or at whitespace. `one of:red 70, green 20, blue 10` weights the choices. A weight is a positive whole number written after the choice. Every choice has a weight, or none do. Without weights, the draw is unchanged.

`decimal:-1.5..1.5 scale 1` draws a fixed-point decimal, a whole count of `10^-n`, never a binary fraction. The scale is 0 through 8, and the text keeps trailing zeros, so money at scale 2 can be `1.50`. `money:0..2` is that generator at scale 2.

`string:adversarial` keeps the ordinary alphabet and, on half the draws, uses a fixed catalog: quotes, injection, markup, a combining mark, a right-to-left mark, an emoji, a zero-width character, and a string of `A` as long as the length allows. `length` still applies, so a short limit leaves the longer payloads out. The result page escapes that text.

`pattern:REG-[0-9]{4}` is literal text plus runs of `[0-9]`, `[A-Z]`, or `[a-z]`. A run is `{n}` with n from 1 through 32, and the identifier is at most 64 characters. It is not a regular expression. Spaces separate the tokens. `size` after a pattern still belongs to the surrounding list.

`all of:red,blue` runs every choice, in that order, instead of sampling. `all of:admin,user x view,edit` is the cross product, and the rightmost group varies fastest: `admin view`, `admin edit`, `user view`, `user edit`. The word `x` is the separator, so `all of:x,y` is two choices and not a product. Several `all of` columns do the same, and the rightmost column varies fastest. The trial count becomes the number of cases. Leave `trials` out, or leave the cell blank, and that count is used. A number you write has to be that count. More than 256 cases is refused. `all of` cannot share the table with a sampled generator, and it cannot sit inside `list of`. The seed is still shown, and it does not change the cases.

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

| property | round trip |
| --- |
| trials | 3 |
| seed | 7 |
| value | decimal:-1.5..1.5 scale 1 |

| property | round trip |
| --- |
| trials | 3 |
| seed | 8 |
| value | money:0..2 |

| property | round trip |
| --- |
| trials | 4 |
| seed | 9 |
| value | one of:red 1, green 1, blue 8 |

| property | round trip |
| --- |
| trials | 3 |
| seed | 10 |
| value | string:adversarial length 0..12 |

| property | round trip |
| --- |
| trials | 3 |
| seed | 11 |
| value | pattern:REG-[0-9]{4} |

| property | round trip |
| --- |
| seed | 12 |
| value | all of:red,blue |

| property | round trip |
| --- |
| seed | 13 |
| value | all of:admin,user x view,edit |

| scenario | pair | left | and | right |
| --- | --- | --- | --- | --- |
| check | echo string | @left | @left |

| property | pair and |
| --- |
| seed | 14 |
| left | all of:a,b |
| right | all of:1,2 |

`all of` replaces sampling, so the two red/blue tables run 2 trials and the two products run 4. A blank trial cell means the same thing.

| script | page driver |
| --- |
| given page | PropertyGuideAll | with content | &#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| script \| agnesse.slim.test.TestSlim \|<br><br>\| scenario \| round trip \| value \|<br>\| check \| echo string \| @value \| @value \|<br><br>\| property \| round trip \|<br>\| seed \| 1 \|<br>\| value \| all of:red,blue \|<br><br>\| property \| round trip \|<br>\| trials \| \|<br>\| seed \| 2 \|<br>\| value \| all of:admin,user x view,edit \|<br><br>\| scenario \| pair \| left \| and \| right \|<br>\| check \| echo string \| @left \| @left \|<br><br>\| property \| pair and \|<br>\| seed \| 3 \|<br>\| left \| all of:a,b \|<br>\| right \| all of:1,2 \| |
| make | PropertyGuideAll | a test page |
| check | request page | PropertyGuideAll?test&format=json | 200 |
| ensure | content matches | "right":3[,}] |
| ensure | content contains | "wrong":0 |
| ensure | content contains | "actual":"2 trials" |
| ensure | content contains | "actual":"4 trials" |

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

The same Python server returns the string it was given from `echo`. The check expects `other`, so the result records the price `1.50`.

| script | page driver |
| --- |
| given page | PropertyGuideMoney | with content | &#96;&#96;&#96;agnesse<br>define COMMAND_PATTERN<br>python3 AgNesseRoot/files/agnesse/tiny_slim.py<br>&#96;&#96;&#96;<br><br>&#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br><br>\| script \| tiny \|<br><br>\| scenario \| show price \| price \|<br>\| check \| echo \| @price \| other \|<br><br>\| property \| show price \|<br>\| trials \| 1 \|<br>\| seed \| 1 \|<br>\| price \| money:1.50..1.50 \| |
| make | PropertyGuideMoney | a test page |
| check | request page | PropertyGuideMoney?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content contains | "right":0 |
| ensure | content contains | "actual":"1.50" |
| ensure | content contains | "expected":"other" |

## Shrinking

After the first failing trial, shrinking sends simpler candidates. Each round is one Slim batch: every simpler value of one input, most aggressive first. The first candidate in that batch that still fails is kept. The page shows one row, the shrunk one, and the trials cell names the original draw, as in `drew [6, 5, 0, 9, 2]; shrunk`. The same seed shrinks to the same row.

A decimal shrinks toward fewer places and toward zero, and it stays inside the range. `1.23` at scale 2 in `-10..10` offers `0.00`, then `1.00`, `1.20`, `0.61`, and `1.22`. A range that does not contain zero shrinks toward the end nearer zero. A weighted `one of` still shrinks toward the first choice. A pattern shrinks toward the first character of each class (`0`, `A`, or `a`). An adversarial string shortens the way any string does, and a cut that would split a surrogate pair is skipped. `all of` shrinks toward the first case. That case is also the first one enumerated, so the first failure is already there and the trials cell does not say `shrunk`.

The walk stops after 32 accepted steps. The trials cell then says `shrink cap`, and the row is the value reached then, which may not be the smallest failure.

A fixture exception counts as a failing candidate, so the walk continues, and that exception is not an extra assertion. A stop-test exception shows the current candidate and sends no further batch.

When the property calls a scenario, a failure renders that scenario's steps for the shrunk input as a child table, the same way a scenario call already does. The round-trip tables above pass, so they have no child table.
