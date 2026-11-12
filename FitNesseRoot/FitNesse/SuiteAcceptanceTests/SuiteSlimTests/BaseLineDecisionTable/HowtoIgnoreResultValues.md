{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
Given you want to **ignore** the results of an output column (like *quotient*)
Then already in the base scenario the column must be empty.


| baseline: fitnesse.slim.test.Division | | | | |
| --- | --- | --- | --- | --- |
| numerator | denominator | quotient? | # Explication | # |
| 10 | 2 | | S1 Base scenario - ignore | . |
| 20 | 4 | 5.0 | S2  correct result | . |
| 20 | 6 | | Ignore | . |
