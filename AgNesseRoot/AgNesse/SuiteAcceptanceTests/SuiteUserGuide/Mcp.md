{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# MCP

`-mcp` serves `list_pages`, `run`, and `get_results` on standard input and output. It does not open a port. Pages stay files. A run's JSON is kept until that process exits. `get_results` returns schema version 1.

Claude, Cursor, and Grok take the same `mcpServers` block:

```json
{
  "mcpServers": {
    "agnesse": {
      "command": "java",
      "args": ["-jar", "agnesse.jar", "-mcp"]
    }
  }
}
```

`run` takes a wiki path. Omit `suite` and a suite page runs its children, a test page runs alone. `tags` and `excludeTags` are the command line's `suiteFilter` and `excludeSuiteFilter`. `rerunFailures` is a file of failed pages. A test-adjunct page is listed and does not run on its own.

| script | mcp session |
| --- | --- |
| check | protocol | 2025-06-18 |
| page; | McpMixed | suite | . |
| page; | McpMixed.McpPass | test | \| script \| agnesse.fixtures.EchoFixture \|<br>\| --- \| --- \| --- \|<br>\| check \| echo \| hello \| hello \| |
| tagged page; | McpMixed.McpLater | test | later | \| script \| agnesse.fixtures.EchoFixture \|<br>\| --- \| --- \| --- \|<br>\| check \| echo \| hello \| hello \| |
| page; | McpMixed.McpFail | test | \| script \| agnesse.fixtures.EchoFixture \|<br>\| --- \| --- \| --- \|<br>\| check \| echo \| hello \| goodbye \| |
| page; | McpMixed.SetUp | test-adjunct | . |
| check | tools | list_pages, run, get_results |
| check | type of | McpMixed | suite |
| check | type of | McpMixed.McpPass | test |
| check | type of | McpMixed.SetUp | test-adjunct |
| check | tags of | McpMixed.McpLater | later |
| check | pages of type | test | 3 |
| check | pages of type | test-adjunct | 1 |
| check | pages with tag | later | 1 |
| check | pages with tag | later, missing | 1 |
| check | pages under | McpMixed | 5 |
| $pass= | run | McpMixed.McpPass |
| ensure | results for | $pass | contain | "schemaVersion": 1 |
| ensure | results for | $pass | contain | "status": "pass" |
| check | failures in | $pass | 0 |
| $suite= | run | McpMixed |
| check | failures in | $suite | 1 |
| ensure | results for | $suite | contain | McpFail |
| $rerun= | rerun | McpMixed |
| check | failures in | $rerun | 1 |
| reject | results for | $rerun | contain | McpPass |
| ensure | results for | $rerun | contain | McpFail |
| $tagged= | run with tags; | McpMixed | later |
| ensure | results for | $tagged | contain | McpLater |
| reject | results for | $tagged | contain | McpFail |
| $excluded= | run excluding; | McpMixed | later |
| reject | results for | $excluded | contain | McpLater |
| ensure | results for | $excluded | contain | McpFail |
