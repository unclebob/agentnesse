{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Property

A property table draws the inputs. It checks one claim against many generated values, instead of a list of examples you type by hand. Name a fixture the way a decision table does. Optional `trials`, `seed`, and `now` rows come next, then a column header and one generator row. An output column ends in `?` or `!` and its generator cell is the expected value.

Reversing a list twice returns the same list. Five trials, one assertion.

| property:agnesse.slim.test.ListReversal |
| --- |
| trials | 5 |
| seed | |
| items | reversed twice? |
| list of int:0..3 size 0..2 | true |

One property is one assertion, however many trials run. A pass colors the trials cell (`5 trials`). The default is 100 trials when that row is omitted or left blank.

The seed is shown on the result and is not written back onto the page. Leave the cell blank and the result fills it in. Copy that number into the cell to draw the same inputs again. A property that draws a time also shows `now`, on the next row, the same way.

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

`date`, `time`, and `datetime` draw ISO-8601 text. `date:2020-01-15..2020-01-15` is that day. `time:08:00:00..08:00:00` is that second. `datetime:2024-06-01T00:00:00Z..2024-06-01T00:00:00Z` is that instant, and the cell carries the offset. A missing range is `2000-01-01` through `2099-12-31` for a date, `00:00:00` through `23:59:59` for a time, and that window at second resolution for a datetime. A time range does not cross midnight.

`zone:America/New_York` overrides the default for that generator. The default is the `zone` key in `agnesse.json`, a `java.time` zone id, and it is not hard-coded. A space sits before the range, as in `datetime zone:America/New_York :2024-06-01T12:00:00Z..2024-06-01T12:00:00Z`, which is `2024-06-01T08:00:00-04:00`. A generator that needs a zone and has neither is refused. The tables below set `zone` to `UTC`.

A bound can be `now` plus a duration. `now+35h59m` is 35 hours and 59 minutes after the clock. `now-1d+2h` stacks pieces, and a space may sit before a sign. On a date the only unit is `d`, a calendar day. On a time or a datetime, `d` is 24 hours.

`now` draws that clock, the same text on every trial. A `now` row sets it. `2024-06-01T00:00:00Z` is an offset datetime. `2024-06-01T00:00:00` is local, and it is read in the configured zone. Leave the cell blank and the clock is `2000-01-01T00:00:00Z` plus the seed, wrapping with an unsigned remainder through the seconds until `2100-01-01T00:00:00Z`. The result shows that instant. It is not written back onto the page. The row has one or two cells. A column named `now` needs a wider header, so `| now | at | past? |` is the header and not a second setting. A scenario reads `$now`. A two-cell input named `now` would set the clock, so a scenario input uses another name.

`period:day`, `period:week`, and `period:month` draw a datetime one second before, at, or one second after a boundary in the default date window. A week starts on Sunday.

A column may name an earlier column. `int after start:0..9` is at least one above `start` and still inside `0..9`. `before` is at least one below. The step is one int, one decimal place, one day, or one second. `money after paid` and `date after start` use that same step. A reference to a later column, an unknown name, or a different kind is refused, and so is a cycle. A repeated name is refused when a column refers to it. An empty window, such as an `int after start:0..20` when `start` is already 20, is an error.

`unique` and `sorted` follow `size` on the list just closed, in either order. `list of int:0..20 size 0..5 unique` draws distinct cells. `sorted` orders ints and decimals as numbers, dates and times chronologically, and strings as text. `unique sorted` is strictly increasing. A `unique` list whose minimum size is larger than a known domain is refused.

`span:0..100 length 1..10` draws `start+length`, the half-open interval from the start for that many steps. `-3+5` starts at -3. `list of span:0..10 length 1..3 size 0..4` is a set of them, and the spans may overlap.

`partition:100 into 3` draws three non-negative ints that add to 100. `partition:10.00 into 3` keeps the scale of the written total, here 2. `into` is 1 through 8. The cell is a Slim list, such as `[10, 0, 0]`.

`monotonic:0..100 size 1..5` is a non-decreasing list of ints. `monotonic:0..100 size 1..5 reset` may drop. A drop counts from the floor again: `[10, 25, 3, 8]` with floor 0 totals 10 + 15 + 3 + 5. Shrinking a reset sequence keeps that total.

`record:name string; age int:0..120` is one JSON object, fields in that order, with no extra spaces. A leaf is a JSON string, so an int cell is `"36"` and money is `"1.50"`. A nested record is parenthesized: `record:name string; doc record:(kind string)`. A field may use `after` or `before` on an earlier field or an earlier column. The field name hides a column of the same name. `all of` cannot be a field.

When the range contains a boundary, half the draws are one of those points and half are ordinary. A date uses the first day and the last day of each month in the range, so a leap day and a year end are in that set. A time uses the local clock time of a DST transition in the zone from 2000 through 2099, the second before it, and the second after it. A datetime uses a DST instant, midnight on the first of a month, the last second of a month, and midnight on a leap day. A range with no such point is uniform. `UTC` has no DST, so a time there is uniform.

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

```agnesse
define zone
UTC
```

The same round trip carries a date, a time, a datetime, a zoned datetime, a duration from the clock, a period boundary, and the clock itself.

| property | round trip |
| --- |
| trials | 1 |
| seed | 20 |
| value | date:2020-01-15..2020-01-15 |

| property | round trip |
| --- |
| trials | 1 |
| seed | 21 |
| value | time:08:00:00..08:00:00 |

| property | round trip |
| --- |
| trials | 1 |
| seed | 22 |
| value | datetime:2024-06-01T00:00:00Z..2024-06-01T00:00:00Z |

| property | round trip |
| --- |
| trials | 1 |
| seed | 23 |
| value | datetime zone:America/New_York :2024-06-01T12:00:00Z..2024-06-01T12:00:00Z |

| property | round trip |
| --- |
| trials | 1 |
| seed | 24 |
| now | 2024-06-01T00:00:00Z |
| value | datetime:now+35h59m..now+35h59m |

| property | round trip |
| --- |
| trials | 1 |
| seed | 0 |
| value | period:day |

| property | round trip |
| --- |
| trials | 1 |
| seed | 0 |
| value | period:week |

| property | round trip |
| --- |
| trials | 1 |
| seed | 0 |
| value | period:month |

| property | round trip |
| --- |
| trials | 1 |
| seed | 25 |
| now | 2024-06-01T00:00:00Z |
| value | now |

`order stays after` echoes the later int, so the check passes. The other tables round-trip a sorted list, a span, a partition, a meter, and a record.

| scenario | order | start | stays after | end |
| --- | --- | --- | --- | --- |
| check | echo string | @end | @end |

| property | order stays after |
| --- |
| trials | 4 |
| seed | 1 |
| start | int:0..5 |
| end | int after start:0..9 |

| property | round trip |
| --- |
| trials | 2 |
| seed | 30 |
| value | list of int:0..12 size 3..3 unique sorted |

| property | round trip |
| --- |
| trials | 2 |
| seed | 31 |
| value | span:-5..5 length 1..4 |

| property | round trip |
| --- |
| trials | 2 |
| seed | 38 |
| value | list of span:0..8 length 1..3 size 2..2 |

| property | round trip |
| --- |
| trials | 2 |
| seed | 32 |
| value | partition:10.00 into 3 |

| property | round trip |
| --- |
| trials | 2 |
| seed | 33 |
| value | monotonic:0..20 size 2..4 |

| property | round trip |
| --- |
| trials | 2 |
| seed | 34 |
| value | monotonic:0..20 size 2..4 reset |

| property | round trip |
| --- |
| trials | 1 |
| seed | 35 |
| value | record:name one of:Ada; age int:36..36 |

| scenario | later day | start | stays after | end |
| --- | --- | --- | --- | --- |
| check | echo string | @end | @end |

| property | later day stays after |
| --- |
| trials | 2 |
| seed | 36 |
| start | date:2020-01-01..2020-01-05 |
| end | date after start:2020-01-01..2020-01-10 |

| property | round trip |
| --- |
| trials | 1 |
| seed | 37 |
| value | record:name one of:Ada; doc record:(kind one of:id; expires date:2020-06-01..2020-06-01) |

`read the clock` sees the same instant the table fixed. `$now` is the clock. `@instant` is the drawn cell.

| scenario | read the clock | instant |
| --- | --- | --- |
| check | echo string | $now | 2024-06-01T00:00:00Z |
| check | echo string | @instant | 2024-06-02T12:00:00Z |

| property | read the clock |
| --- |
| trials | 1 |
| seed | 1 |
| now | 2024-06-01T00:00:00Z |
| instant | datetime:2024-06-02T12:00:00Z..2024-06-02T12:00:00Z |

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

A date, a time, or a datetime shrinks toward the clock when the clock sits in the range, and toward the start of the range otherwise. Whole units come first: a year, a month, or a day; an hour or a minute; or a local day, hour, or minute. The candidate stays inside the range. A period that is one second off its boundary shrinks onto the boundary. `datetime:now+35h59m..now+36h01m`, with the clock at `2024-06-01T00:00:00Z` and seed 1, draws `2024-06-02T12:00:41Z` and shrinks to `2024-06-02T12:00:00Z`.

`after` and `before` shrink inside the live window, so a column never crosses the one it names. Shrinking the named column is kept only when the later cells still hold. A span shortens its length toward the minimum, then moves the start toward zero. A partition moves later parts onto the first and keeps the total, so the simplest row is the total followed by zeros. A monotonic list shortens toward the floor. With `reset`, the shortest sequence with the same running total is tried first. A unique or sorted list drops a candidate that would repeat or fall out of order. A record shrinks one field at a time, and a later field that would break is left as it is.

The walk stops after 32 accepted steps. The trials cell then says `shrink cap`, and the row is the value reached then, which may not be the smallest failure.

A fixture exception counts as a failing candidate, so the walk continues, and that exception is not an extra assertion. A stop-test exception shows the current candidate and sends no further batch.

When the property calls a scenario, a failure renders that scenario's steps for the shrunk input as a child table, the same way a scenario call already does. The round-trip tables above pass, so they have no child table.
