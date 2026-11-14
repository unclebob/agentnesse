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
