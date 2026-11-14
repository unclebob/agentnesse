{
  "type": "test"
}

```agentnesse
define TEST_SYSTEM
slim
```

A comparison is an assertion. A fixture that never starts is a table failure, and the page is not a pass.

| script | page driver |
| --- | --- |
| given page | CountAssertions.Mixed | with content | &#96;&#96;&#96;agentnesse<br>define TEST_SYSTEM<br>slim<br>&#96;&#96;&#96;<br><br>\| script \| fitnesse.fixtures.EchoFixture \|<br>\| --- \| --- \|<br>\| check \| echo \| ok \| ok \|<br>\| check \| echo \| ok \| bad \| |
| make | CountAssertions.Mixed | a test page |
| check | request page | CountAssertions.Mixed?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content contains | "right":1 |
| ensure | content contains | "tableFailures":0 |
| ensure | content contains | "status":"fail" |
| given page | CountAssertions.Broken | with content | &#96;&#96;&#96;agentnesse<br>define TEST_SYSTEM<br>slim<br>&#96;&#96;&#96;<br><br>\| script \| NoSuchFixtureForTableFailure \| |
| make | CountAssertions.Broken | a test page |
| check | request page | CountAssertions.Broken?test&format=json | 200 |
| ensure | content contains | "tableFailures":1 |
| ensure | content contains | "table-failure" |
| ensure | content contains | "status":"error" |
| ensure | content contains | "right":0 |
| ensure | content contains | "exceptions":0 |
| given page | CountAssertions.OneFail | with content | &#96;&#96;&#96;agentnesse<br>define TEST_SYSTEM<br>slim<br>&#96;&#96;&#96;<br><br>\| table: fitnesse.slim.test.CellResultTable \|<br>\| --- \|<br>\| fail \| |
| make | CountAssertions.OneFail | a test page |
| check | request page | CountAssertions.OneFail?test&format=json | 200 |
| ensure | content contains | "wrong":1 |
| ensure | content contains | "right":0 |
| ensure | content contains | "status":"fail" |
| given page | CountAssertions.NoCells | with content | &#96;&#96;&#96;agentnesse<br>define TEST_SYSTEM<br>slim<br>&#96;&#96;&#96;<br><br>\| table: fitnesse.slim.test.CellResultTable \|<br>\| --- \|<br>\| no change \| |
| make | CountAssertions.NoCells | a test page |
| check | request page | CountAssertions.NoCells?test&format=json | 200 |
| ensure | content contains | "ignored":1 |
| ensure | content contains | "right":0 |
| ensure | content contains | "wrong":0 |
| ensure | content contains | "status":"ignore" |
| given page | CountAssertions.OnePass | with content | &#96;&#96;&#96;agentnesse<br>define TEST_SYSTEM<br>slim<br>&#96;&#96;&#96;<br><br>\| table: fitnesse.fixtures.TableOrDecisionFixture \|<br>\| --- \| --- \|<br>\| parsing as? \| Parsed as Table fixture \| |
| make | CountAssertions.OnePass | a test page |
| check | request page | CountAssertions.OnePass?test&format=json | 200 |
| ensure | content contains | "right":1 |
| ensure | content contains | "wrong":0 |
