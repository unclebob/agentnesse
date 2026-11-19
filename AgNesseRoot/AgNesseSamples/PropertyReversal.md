{
  "type": "test",
  "tags": ["sample"],
  "help": "A property table",
  "prune": false
}
# Reversal holds for every list

A property checks one claim against generated inputs. This one says that reversing a list twice returns the list. Twenty trials, one assertion. Leave `seed` blank and the result shows the seed that was used. Copy that number back into the cell to draw the same lists again. A failure would append one shrunk row and name the original draw in the trials cell. This page passes, so it shows no counterexample.

| property:agnesse.slim.test.ListReversal |
| --- |
| trials | 20 |
| seed | |
| items | reversed twice? |
| list of int:-5..5 size 0..4 | true |
