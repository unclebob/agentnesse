{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Alias

An `alias` block names a table. The first line is the name used on the page. The next line is the fixture or table type it runs. A one-line alias does nothing.

| import |
| --- |
| agnesse.slim.test |

```agnesse
alias split
Division
```

| split |
| --- |
| numerator | denominator | quotient? |
| 10 | 2 | 5.0 |
