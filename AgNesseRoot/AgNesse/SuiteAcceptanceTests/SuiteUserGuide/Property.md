{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Property

A property table draws the inputs. It names a fixture the way a decision table does, then optional `trials` and `seed` rows, then a header and one generator row. The outputs are ordinary expectations. One property is one assertion, however many trials run. The default is 100 trials. The seed is shown on the result. The saved page keeps the seed cell you wrote.

```
| property:agnesse.slim.test.ListReversal |
| trials | 5 |
| seed | |
| items | reversed twice? |
| list of int:0..3 size 0..2 | true |
```

A passing result colors the trials cell (`5 trials`) and fills the seed. A failing result appends the first failing row, unshrunk, and leaves the other trials off the page. Put that seed back in the cell to draw the same inputs again.

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
