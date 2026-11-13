{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
A symbol assigned from HTML is shown as that HTML, not as a rendered table.

| script | fitnesse.fixtures.EchoFixture | |
| --- | --- | --- |
| $Table= | echo | <table><tr><td>Test</td></tr></table> |
| check | echo | $Table | $Table |
| show | echo | $Table |
