{
  "type": "test",
  "tags": ["sample"],
  "help": "A test that names its SetUp",
  "prune": false
}
# With SetUp

```agentnesse
setup
Run the sibling SetUp page first.
```

```agentnesse
include SetUp
```

| query:Employees | name | hired |
| --- | --- | --- |
| hired before? | Bob | 10-Oct-1974 |
