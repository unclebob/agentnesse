{
  "type": "test",
  "tags": [],
  "help": null,
  "prune": false
}
# Scenarios

A `scenarios` block loads the named page before the tables that follow. The scenario table on that page becomes a script row here. `@name` is replaced in that row, so the result is `hello world`, not the template text.

```agnesse
scenarios ScenariosPage
```

| script | echo fixture |
| --- | --- |
| greet | world |

| script | page driver |
| --- | --- |
| given page | GuideScenario | with content | <br><br>\| scenario \| greet \| name \|<br>\| --- \| --- \| --- \|<br>\| check \| echo \| hello @name \| hello world \| |
| given page | GuideGreet | with content | &#96;&#96;&#96;agnesse<br>define SLIM_PORT<br>1<br>&#96;&#96;&#96;<br>&#96;&#96;&#96;agnesse<br>scenarios GuideScenario<br>&#96;&#96;&#96;<br><br>\| script \| agnesse.fixtures.EchoFixture \|<br>\| --- \| --- \|<br>\| greet \| world \| |
| make | GuideGreet | a test page |
| check | request page | GuideGreet?test&format=json | 200 |
| ensure | content contains | "actual":"hello world" |
| ensure | content contains | "expected":"hello world" |
| ensure | content matches | "right":1[,}] |
| reject | content contains | "actual":"hello @name" |
