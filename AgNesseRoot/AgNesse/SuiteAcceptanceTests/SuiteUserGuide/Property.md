{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Property

A property table draws the inputs. It names a fixture the way a decision table does, then optional `trials` and `seed` rows, then a header and one generator row. The outputs are ordinary expectations. One property is one assertion, however many trials run. The default is 100 trials. The seed is shown on the result. The saved page keeps the seed cell you wrote, and it does not save the counterexample.

```
| property:agnesse.slim.test.ListReversal |
| trials | 5 |
| seed | |
| items | reversed twice? |
| list of int:0..3 size 0..2 | true |
```

A passing result colors the trials cell (`5 trials`) and fills the seed. A failing result appends one row, the shrunk counterexample, and leaves the other trials off the page. The trials cell names the original draw, for example `trial 1 of 1 drew [6, 5, 0, 9, 2]; shrunk`. Each shrink round sends every simpler candidate in one Slim batch and keeps the first that still fails. The same seed shrinks to the same row.

Shrinking stops after 32 accepted steps. The trials cell then says `shrink cap` and the row is the value reached, not necessarily the smallest failure. A fixture exception counts as a failing candidate, so the walk continues, and it does not add an assertion. A stop-test exception shows the current candidate and sends no further batch.

When the property calls a scenario, the shrunk input is rendered as that scenario's steps, in a child table, the way a scenario call already renders. Put the seed back in the cell to draw the same inputs again.

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
