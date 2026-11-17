{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
One of the key goals of `AgNesse` is to encourage that requirements in `AgNesse` are written before the coding of the implementation starts.
Having said that the defintion of the requirements must be independent of the technical solution used to implement it.
This sample page shows that the same requirement defintion can be implemented once with a java class implementing the decision table protokoll and once with a senario.


The requirements are defined in a decision table on a sub page. 
The first implementation used is the Division class from the agnesse.slim.test package.

| import |
| --- |
| agnesse.slim.test |

```agnesse
include >CalculatorRequirements
```


Now we define a second technical solution using a `AgNesse` scenario

| scenario | Division _ _ _ | numerator, denominator, quotient? |
| --- | --- | --- |
| setNumerator | @numerator | |
| setDenominator | @denominator | |
| $quotient= | quotient | |


Generate an instance of the class used by the scenario:

| Library |
| --- |
| agnesse.slim.test.Division |



We retest the requirements and get the same results.

```agnesse
include >CalculatorRequirements
```
