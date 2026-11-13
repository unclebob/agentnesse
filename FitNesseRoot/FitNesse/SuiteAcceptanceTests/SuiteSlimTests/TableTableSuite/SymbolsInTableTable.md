{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A table table can read symbols and assign new ones. An assignment is not visible to a later row of the same table.

| import |
| --- |
| fitnesse.slim.test |


| script | | |
| --- | --- | --- |
| $A= | echo int | 99 |
| $B= | echo int | 5 |
| $C= | echo int | 14 |


| table: Table Table Inc First Col | |
| --- | --- |
| $A | $resultA= |
| $B | $resultB= |
| $C | $C= |


| script | | | |
| --- | --- | --- | --- |
| check | echo int | $resultA | 100 |
| check | echo int | $resultB | 6 |
| check | echo int | $C | 15 |


```agentnesse
include <SuiteAcceptanceTests.SuiteSlimTests.TableTableSuite.SymbolAssignmentAndReferenceOfTheSameInOneTable
```

Z stays 45, because the assignment in the included table is not reused on the next row.

| script | | | |
| --- | --- | --- | --- |
| check | echo int | $Z | 45 |
