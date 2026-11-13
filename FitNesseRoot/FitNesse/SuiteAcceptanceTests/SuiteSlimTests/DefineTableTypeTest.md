{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
Purpose: An alias can choose the table type for a fixture name.

By default table should be parsed as a decision table.

| Table Or Decision Fixture |
| --- |
| parsing as? |
| Parsed as Decision fixture |


However after this table it should be parsed as a **Table: table**.


```agentnesse
alias Table or Decision Fixture
table:
```



| Table Or Decision Fixture | |
| --- | --- |
| parsing as? | Parsed as Table fixture |

The same works for script tables (which parse as decision tables in many respects).


```agentnesse
alias Table or Decision Fixture
script:
```


| Table Or Decision Fixture | | |
| --- | --- | --- |
| check | parsing as | Parsed as Decision fixture |
