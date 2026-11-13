{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A table table whose fixture handles symbols can use a value assigned earlier in the same table.

| import |
| --- |
| fitnesse.slim.test |
| fitnesse.slim.test.statementexecutorconsumer |


```agentnesse
include <SuiteAcceptanceTests.SuiteSlimTests.TableTableSuite.SymbolAssignmentAndReferenceOfTheSameInOneTable
```

| script | | | |
| --- | --- | --- | --- |
| check | echo int | $Z | 201 |


| script | | |
| --- | --- | --- |
| $X= | echo | 99 |


| table: Table Table Inc First Col | |
| --- | --- |
| $X | $X= |
| $X | $X= |
| $X | $X= |
| $X | $X= |
| $X | $X= |
| $X | $Y= |


| script | | | |
| --- | --- | --- | --- |
| check | echo | $X | 104 |
| check | echo | $Y | 105 |
